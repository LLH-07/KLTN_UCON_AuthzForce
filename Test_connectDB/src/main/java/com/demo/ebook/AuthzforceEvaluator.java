/*
    File này chứa logic đánh giá chính sách bằng Authzforce
    
    Nó làm 4 việc chính:
        1. Nạp cấu hình PDP từ file pdp.xml
                - PdpEngineConfiguration conf = PdpEngineConfiguration.getInstance(pdpConf, catalog, extXsd);
                        + pdpConf là file pdp.xml
                        + catalog là file xml-catalog.xml để ánh xạ các namespace sang file XSD
                        + extXsd là file XSD mở rộng để định nghĩa cấu trúc XML cho custom policy provider
        2. Tạo PDP engine với cấu hình đó để đánh giá request: BasePdpEngine pdp = new BasePdpEngine(conf);
        3. Xây dựng request context từ session_id (dùng RequestContextRepository để đọc dữ liệu liên quan đến session_id từ database)
                - Nó tạo DecisionRequestBuilder, rồi đi đọc từ DB ra các dữ kiện như:
                        + subject:status
                        + subject:is_visitor
                        + resource:status
                        + resource:preview_chunk_limit
                        + env:access_path
                        + env:requested_chunk
                        + action:action_id = read
                - Đây chính là request context mà PDP engine sẽ đánh giá dựa trên các policy đã nạp vào ở bước 1
        4. Đánh giá request context với PDP engine, trả về kết quả Permit/Deny
*/

package com.demo.ebook;

import java.util.Optional;

import org.ow2.authzforce.core.pdp.api.AttributeFqn;
import org.ow2.authzforce.core.pdp.api.AttributeFqns;
import org.ow2.authzforce.core.pdp.api.DecisionRequestBuilder;
import org.ow2.authzforce.core.pdp.api.value.AttributeBag;
import org.ow2.authzforce.core.pdp.api.value.Bags;
import org.ow2.authzforce.core.pdp.api.value.BooleanValue;
import org.ow2.authzforce.core.pdp.api.value.IntegerValue;
import org.ow2.authzforce.core.pdp.api.value.StandardDatatypes;
import org.ow2.authzforce.core.pdp.api.value.StringValue;
import org.ow2.authzforce.core.pdp.impl.BasePdpEngine;
import org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration;
import org.ow2.authzforce.xacml.identifiers.XacmlAttributeCategory;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;

public class AuthzforceEvaluator {
    private final RequestContextRepository repo = new RequestContextRepository();

    public String evaluate(int sessionId, int requestedChunk) throws Exception {
        var ctx = repo.loadBySessionId(sessionId);

        String pdpConf = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/pdp.xml";
        String catalog = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/xml-catalog.xml";
        String extXsd = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/pdp-ext.xsd";

        System.setProperty("javax.xml.accessExternalSchema", "file");
        System.setProperty("javax.xml.accessExternalDTD", "file");

        PdpEngineConfiguration conf = PdpEngineConfiguration.getInstance(pdpConf, catalog, extXsd);
        BasePdpEngine pdp = new BasePdpEngine(conf);

        DecisionRequestBuilder<?> requestBuilder = pdp.newRequestBuilder(-1, -1);

        putString(
                requestBuilder,
                XacmlAttributeCategory.XACML_1_0_ACCESS_SUBJECT.value(),
                "subject:status",
                ctx.subjectStatus()
        );

        putBoolean(
                requestBuilder,
                XacmlAttributeCategory.XACML_1_0_ACCESS_SUBJECT.value(),
                "subject:is_visitor",
                ctx.subjectIsVisitor()
        );

        putString(
                requestBuilder,
                XacmlAttributeCategory.XACML_3_0_RESOURCE.value(),
                "resource:status",
                ctx.resourceStatus()
        );

        putInteger(
                requestBuilder,
                XacmlAttributeCategory.XACML_3_0_RESOURCE.value(),
                "resource:preview_chunk_limit",
                ctx.previewChunkLimit()
        );

        putString(
                requestBuilder,
                XacmlAttributeCategory.XACML_3_0_ENVIRONMENT.value(),
                "env:access_path",
                ctx.accessPath()
        );

        putInteger(
                requestBuilder,
                XacmlAttributeCategory.XACML_3_0_ENVIRONMENT.value(),
                "env:requested_chunk",
                requestedChunk
        );

        putString(
                requestBuilder,
                XacmlAttributeCategory.XACML_3_0_ACTION.value(),
                "urn:oasis:names:tc:xacml:1.0:action:action-id",
                "read"
        );

        var request = requestBuilder.build(false);
        var result = pdp.evaluate(request);
        DecisionType decision = result.getDecision();

        return decision.value();
    }

    private static void putString(DecisionRequestBuilder<?> requestBuilder, String category, String attrId, String value) {
        AttributeFqn fqn = AttributeFqns.newInstance(category, Optional.empty(), attrId);
        AttributeBag<?> bag = Bags.singletonAttributeBag(StandardDatatypes.STRING, new StringValue(value));
        requestBuilder.putNamedAttributeIfAbsent(fqn, bag);
    }

    private static void putBoolean(DecisionRequestBuilder<?> requestBuilder, String category, String attrId, boolean value) {
        AttributeFqn fqn = AttributeFqns.newInstance(category, Optional.empty(), attrId);
        AttributeBag<?> bag = Bags.singletonAttributeBag(
                StandardDatatypes.BOOLEAN,
                value ? BooleanValue.TRUE : BooleanValue.FALSE
        );
        requestBuilder.putNamedAttributeIfAbsent(fqn, bag);
    }

    private static void putInteger(DecisionRequestBuilder<?> requestBuilder, String category, String attrId, int value) {
        AttributeFqn fqn = AttributeFqns.newInstance(category, Optional.empty(), attrId);
        AttributeBag<?> bag = Bags.singletonAttributeBag(StandardDatatypes.INTEGER, IntegerValue.valueOf(value));
        requestBuilder.putNamedAttributeIfAbsent(fqn, bag);
    }
}