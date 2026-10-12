/*
File RequestContextRepository.java đọc dữ liệu từ:
- reading_sessions(session_id, user_id, book_id, start_time)
- users_demo(user_id, status, is_visitor) # để test demo, không có bảng user chính thức nào
- books(book_id, status, preview_chunk_limit)

Từ đó, nó sẽ trả ra 1 Object RequestContext chứa các trường:
- subjectStatus (string)
- subjectIsVisitor (boolean)
- resourceStatus (string)
- previewChunkLimit (int)
- accessPath (string)

Nói một cách dễ hiểu: File này gom các dữ liệu liên quan đến 1 request đọc sách để gửi cho AuthzforceEvaluator

*/

package com.demo.ebook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RequestContextRepository {

    public record RequestContext(
            String subjectStatus,
            boolean subjectIsVisitor,
            String resourceStatus,
            int previewChunkLimit,
            String accessPath
    ) {}

    public RequestContext loadBySessionId(int sessionId) throws Exception {
        String sql = """
            select u.status as subject_status,
                   u.is_visitor as subject_is_visitor,
                   b.status as resource_status,
                   b.preview_chunk_limit as preview_chunk_limit,
                   rs.access_path as access_path
            from reading_sessions rs
            join users_demo u on u.user_id = rs.user_id
            join books b on b.book_id = rs.book_id
            where rs.session_id = ?
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Khong tim thay session_id=" + sessionId);
                }
                return new RequestContext(
                        rs.getString("subject_status"),
                        rs.getBoolean("subject_is_visitor"),
                        rs.getString("resource_status"),
                        rs.getInt("preview_chunk_limit"),
                        rs.getString("access_path")
                );
            }
        }
    }
}