/*
File này định nghĩa cấu hình cho custom policy provider, bao gồm: 
- Các thông tin cần thiết để kết nối đến database
- Truy vấn policy XML. 

Cấu hình này sẽ được đọc từ file XML config của Authzforce PDP, sau đó được sử dụng để khởi tạo PostgresPolicyProvider.

Nó là lớp JAVAX-B, được ánh xạ với cấu trúc XML trong file pdp-ext.xsd. Cụ thể, nó có 3 thuộc tính:
- jdbcUrl: JDBC URL để kết nối đến database PostgreSQL chứa policy XML
- jdbcUser: Username để kết nối đến database
- jdbcPassword: Password để kết nối đến database

Nó kế thừa AbstractPolicyProvider, và được ánh xạ với namespace "urn:demo:authzforce:dbprovider" trong file pdp-ext.xsd và xml-catalog.xml. 
Điều này cho phép chúng ta sử dụng cấu hình này trong file pdp.xml để khởi tạo PostgresPolicyProvider.



Nói dễ hiểu:
- pdp.xml khai báo provider bằng XML
- Authzforce sẽ deserialize XML đó thành PostgresPolicyProviderDescriptor
- PostgresPolicyProviderDescriptor chứa thông tin kết nối DB và truy vấn policy XML

*/

package com.demo.authz.dbprovider;

import org.ow2.authzforce.xmlns.pdp.ext.AbstractPolicyProvider;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PostgresPolicyProviderDescriptor", namespace = "urn:demo:authzforce:dbprovider")
public class PostgresPolicyProviderDescriptor extends AbstractPolicyProvider {

    @XmlAttribute(name = "jdbcUrl", required = true)
    private String jdbcUrl;

    @XmlAttribute(name = "jdbcUser", required = true)
    private String jdbcUser;

    @XmlAttribute(name = "jdbcPassword", required = true)
    private String jdbcPassword;

    public PostgresPolicyProviderDescriptor() {
    }

    public PostgresPolicyProviderDescriptor(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }
}