/*
Đây là file quan trọng nhất của phần policy

Vai trò chính: Custome policy provider, đọc policy XML từ database PostgreSQL, rồi trả về cho AuthzforceEvaluator để đánh giá.

Theo API của Authzforce:
- CloseablePolicyProvider.Fatory<T> là một Policy Provider
- Factory của nó nhận vào Descriptor (đã được deserialize từ XML config), parser factory, expression factory, combining algorithm registry, environment properties, và các tham số khác 
   --> trả về instance của PostgresPolicyProvider đã được khởi tạo với config đó.
- Policy provider có method get() và getCandidateRootPolicy() để trả về policy element evaluator cho PDP engine khi đánh giá request.

Trong bản demo hiện tại, nó làm theo cách đơn giản nhất:
1. query DB lấy XML policy
2. parse XML thành PolicySet
3. dựng một built-in StaticPolicyProvider
4. giao phần parse/compile runtime policy cho built-in provider đó

Vì built-in StaticPolicyProvider của AuthzForce đã hỗ trợ policy theo kiểu:
- PolicySet
- hoặc policyLocation
Nên ta tận dụng nó thay vì tự viết engine parse policy.


Trong file này có 5 đoạn chính:
1. constructor PostgresPolicyProvider: nhận config, gọi buildDelegate để khởi tạo delegate policy provider
2. buildDelegate: 
    - Đọc root policy XML từ DB
    - Parse thành PolicySet
    - Khởi tạo built-in StaticPolicyProvider với PolicySet đó
    - Tìm built-in tatic provider factory bằng ServiceLoader, rồi gọi getInstance để tạo provider đó làm delegate
3. loadRootPolicyXml: query DB để lấy XML policy
4. parsePolicySet: parse XML string thành PolicySet object bằng JAXB
5. Các method joinPolicyRefChains, get, getCandidateRootPolicy, close: đơn giản là gọi delegate tương ứng



Trong demo này, custom PostgresPolicyProvider chạy query SQL ở trên để lấy xml_body, rồi biến nó thành PolicySet
*/

package com.demo.authz.dbprovider;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.ow2.authzforce.core.pdp.api.EnvironmentProperties;
import org.ow2.authzforce.core.pdp.api.EvaluationContext;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.JaxbBoundPdpExtension;
import org.ow2.authzforce.core.pdp.api.PdpExtension;
import org.ow2.authzforce.core.pdp.api.XmlUtils.XmlnsFilteringParserFactory;
import org.ow2.authzforce.core.pdp.api.combining.CombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.api.expression.ExpressionFactory;
import org.ow2.authzforce.core.pdp.api.policy.CloseablePolicyProvider;
import org.ow2.authzforce.core.pdp.api.policy.PolicyProvider;
import org.ow2.authzforce.core.pdp.api.policy.PolicyVersionPatterns;
import org.ow2.authzforce.core.pdp.api.policy.PrimaryPolicyMetadata;
import org.ow2.authzforce.core.pdp.api.policy.TopLevelPolicyElementEvaluator;
import org.ow2.authzforce.core.pdp.api.policy.TopLevelPolicyElementType;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySet;

public final class PostgresPolicyProvider implements CloseablePolicyProvider<TopLevelPolicyElementEvaluator> {

    private final CloseablePolicyProvider<TopLevelPolicyElementEvaluator> delegate;

    public PostgresPolicyProvider(
            PostgresPolicyProviderDescriptor conf,
            XmlnsFilteringParserFactory xacmlParserFactory,
            int maxPolicySetRefDepth,
            ExpressionFactory expressionFactory,
            CombiningAlgRegistry combiningAlgRegistry,
            EnvironmentProperties environmentProperties,
            Optional<PolicyProvider<?>> otherHelpingPolicyProvider
    ) {
        try {
            this.delegate = buildDelegate(
                    conf,
                    xacmlParserFactory,
                    maxPolicySetRefDepth,
                    expressionFactory,
                    combiningAlgRegistry,
                    environmentProperties,
                    otherHelpingPolicyProvider
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot initialize PostgresPolicyProvider. "
                            + "Check JDBC URL/user/password, database 'ebook_demo', schema 'public', "
                            + "and tables public.xacml_policies + public.xacml_root_pointer.",
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private CloseablePolicyProvider<TopLevelPolicyElementEvaluator> buildDelegate(
            PostgresPolicyProviderDescriptor conf,
            XmlnsFilteringParserFactory xacmlParserFactory,
            int maxPolicySetRefDepth,
            ExpressionFactory expressionFactory,
            CombiningAlgRegistry combiningAlgRegistry,
            EnvironmentProperties environmentProperties,
            Optional<PolicyProvider<?>> otherHelpingPolicyProvider
    ) throws Exception {

        String rootXml = loadRootPolicyXml(conf);

        List<Object> inlinePolicySets = new ArrayList<>();
        inlinePolicySets.add(parsePolicySet(rootXml));

        org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider staticConf =
                new org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider(inlinePolicySets, false);

        CloseablePolicyProvider.Factory<org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider> builtInFactory =
                findBuiltInStaticPolicyProviderFactory();

        return (CloseablePolicyProvider<TopLevelPolicyElementEvaluator>) builtInFactory.getInstance(
                staticConf,
                xacmlParserFactory,
                maxPolicySetRefDepth,
                expressionFactory,
                combiningAlgRegistry,
                environmentProperties,
                otherHelpingPolicyProvider
        );
    }

    @SuppressWarnings("unchecked")
    private CloseablePolicyProvider.Factory<org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider>
    findBuiltInStaticPolicyProviderFactory() {

        ServiceLoader<PdpExtension> loader = ServiceLoader.load(PdpExtension.class);

        for (PdpExtension ext : loader) {
            if (ext instanceof CloseablePolicyProvider.Factory<?> factory
                    && ext instanceof JaxbBoundPdpExtension<?> jaxbExt
                    && jaxbExt.getJaxbClass().equals(org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider.class)) {

                return (CloseablePolicyProvider.Factory<org.ow2.authzforce.core.xmlns.pdp.StaticPolicyProvider>) factory;
            }
        }

        throw new IllegalStateException("Built-in StaticPolicyProvider factory not found via ServiceLoader");
    }

    private String loadRootPolicyXml(PostgresPolicyProviderDescriptor conf) throws Exception {
        System.out.println("POLICY DEBUG => loading root policy XML from DB");
        String sql = """
                select p.xml_body
                from public.xacml_root_pointer rp
                join public.xacml_policies p
                  on p.policy_id = rp.root_policy_id
                 and p.version_no = rp.root_policy_version
                where p.is_active = true
                """;

        try (Connection c = DriverManager.getConnection(conf.getJdbcUrl(), conf.getJdbcUser(), conf.getJdbcPassword());
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new IllegalStateException("No root policy found in public.xacml_root_pointer/public.xacml_policies");
            }

            return rs.getString("xml_body");
        }
    }

    private PolicySet parsePolicySet(String xmlBody) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PolicySet.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object obj = unmarshaller.unmarshal(new StringReader(xmlBody));

        if (obj instanceof PolicySet policySet) {
            return policySet;
        }

        if (obj instanceof JAXBElement<?> jaxbElement && jaxbElement.getValue() instanceof PolicySet policySet) {
            return policySet;
        }

        throw new IllegalArgumentException("Policy XML in DB is not a PolicySet");
    }

    @Override
    public Deque<String> joinPolicyRefChains(Deque<String> policyRefChain1, List<String> policyRefChain2)
            throws IllegalArgumentException {
        return delegate.joinPolicyRefChains(policyRefChain1, policyRefChain2);
    }

    @Override
    public TopLevelPolicyElementEvaluator get(
            TopLevelPolicyElementType policyType,
            String policyId,
            Optional<PolicyVersionPatterns> policyVersionConstraints,
            Deque<String> policySetRefChain,
            EvaluationContext evaluationCtx,
            Optional<EvaluationContext> mdpContext
    ) throws IllegalArgumentException, IndeterminateEvaluationException {
        return delegate.get(policyType, policyId, policyVersionConstraints, policySetRefChain, evaluationCtx, mdpContext);
    }

    @Override
    public Optional<PrimaryPolicyMetadata> getCandidateRootPolicy() {
        return delegate.getCandidateRootPolicy();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}