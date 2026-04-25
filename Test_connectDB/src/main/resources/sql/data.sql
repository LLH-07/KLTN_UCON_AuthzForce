insert into users_demo(username, status, is_visitor)
values
('alice', 'ACTIVE', false),
('bob', 'INACTIVE', false),
('guest_1', 'INACTIVE', true);

insert into books(title, status, preview_chunk_limit)
values
('De Men Phieu Luu Ky', 'ACTIVE', 5),
('Khoa Luan UCON', 'ACTIVE', 3);

insert into reading_sessions(user_id, book_id, access_path)
values
(1, 1, 'PREVIEW'),
(2, 1, 'PREVIEW'),
(3, 1, 'PREVIEW'),
(1, 2, 'FREE');

insert into xacml_policies(policy_id, version_no, xml_body, is_active)
values
(
  'root-preview-policy',
  '1.0',
  $$PASTE_TOAN_BO_NOI_DUNG_POLICY_PREVIEW_XML_O_DAY$$,
  true
);

insert into xacml_root_pointer(root_policy_id, root_policy_version)
values ('root-preview-policy', '1.0');