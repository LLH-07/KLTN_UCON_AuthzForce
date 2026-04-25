/*
Theo contract của Authzforce, mọi custom policy provider phải có Factory kế thừa từ CloseablePolicyProvider.Factory, và Descriptor kế thừa từ AbstractPolicyProvider.

File này làm 2 việc:
- `getJaxbClass()`: trả về class của Descriptor, để Authzforce biết dùng class nào để deserialize XML config thành object
    + Ý nghĩa: nói với AuthzForce rằng provider này gắn với JAXB config class nào JaxbBoundPdpExtension quy định mọi JAXB-bound extension phải có getJaxbClass(), và getId() mặc định lấy canonical name của class đó.
- `getInstance()`: nhận vào Descriptor đã được deserialize từ XML config, cùng với các tham số cần thiết khác, rồi trả về instance của PostgresPolicyProvider đã được khởi tạo với config đó.

*/

package com.demo.authz.dbprovider;

import java.util.Optional;

import org.ow2.authzforce.core.pdp.api.EnvironmentProperties;
import org.ow2.authzforce.core.pdp.api.XmlUtils.XmlnsFilteringParserFactory;
import org.ow2.authzforce.core.pdp.api.combining.CombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.api.expression.ExpressionFactory;
import org.ow2.authzforce.core.pdp.api.policy.CloseablePolicyProvider;
import org.ow2.authzforce.core.pdp.api.policy.PolicyProvider;

public final class PostgresPolicyProviderFactory
        extends CloseablePolicyProvider.Factory<PostgresPolicyProviderDescriptor> {

    public PostgresPolicyProviderFactory() {
    }

    @Override
    public Class<PostgresPolicyProviderDescriptor> getJaxbClass() {
        return PostgresPolicyProviderDescriptor.class;
    }

    @Override
    public CloseablePolicyProvider<?> getInstance(
            PostgresPolicyProviderDescriptor conf,
            XmlnsFilteringParserFactory xacmlParserFactory,
            int maxPolicySetRefDepth,
            ExpressionFactory expressionFactory,
            CombiningAlgRegistry combiningAlgRegistry,
            EnvironmentProperties environmentProperties,
            Optional<PolicyProvider<?>> otherHelpingPolicyProvider
    ) throws IllegalArgumentException {

        if (conf == null) {
            throw new IllegalArgumentException("PostgresPolicyProviderDescriptor must not be null");
        }

        return new PostgresPolicyProvider(
                conf,
                xacmlParserFactory,
                maxPolicySetRefDepth,
                expressionFactory,
                combiningAlgRegistry,
                environmentProperties,
                otherHelpingPolicyProvider
        );
    }
}