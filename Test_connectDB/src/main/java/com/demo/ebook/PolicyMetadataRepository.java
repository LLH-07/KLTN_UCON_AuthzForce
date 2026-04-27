package com.demo.ebook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PolicyMetadataRepository {

    public PolicyFingerprint loadCurrentFingerprint() throws Exception {
        String sql = """
            select rp.root_policy_id,
                   rp.root_policy_version,
                   p.updated_at
            from public.xacml_root_pointer rp
            join public.xacml_policies p
              on p.policy_id = rp.root_policy_id
             and p.version_no = rp.root_policy_version
            where p.is_active = true
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new IllegalStateException("Không tìm thấy root policy active trong DB");
            }

            return new PolicyFingerprint(
                    rs.getString("root_policy_id"),
                    rs.getString("root_policy_version"),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}

/*
File này không đọc full XML.
Nó chỉ đọc metadata nhẹ để biết policy có đổi chưa.
*/
