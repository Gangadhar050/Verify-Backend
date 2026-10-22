
package com.verify_x.services;

import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Locale;

@Component
public class EmailDomainValidator {

    public boolean hasMailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1)
                .toLowerCase(Locale.ROOT);

        Hashtable<String, String> env = new Hashtable<>();
        env.put(
                "java.naming.factory.initial",
                "com.sun.jndi.dns.DnsContextFactory"
        );
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            DirContext context = new InitialDirContext(env);

            try {
                if (hasRecord(context, domain, "MX")) {
                    return true;
                }

                // Some valid mail domains use implicit MX routing.
                return hasRecord(context, domain, "A")
                        || hasRecord(context, domain, "AAAA");
            } finally {
                context.close();
            }
        } catch (NamingException ex) {
            // DNS failure is not proof that the domain is invalid.
            throw new IllegalStateException(
                    "Unable to verify email domain right now.", ex
            );
        }
    }

    private boolean hasRecord(
            DirContext context,
            String domain,
            String recordType
    ) throws NamingException {

        Attributes attributes =
                context.getAttributes(domain, new String[]{recordType});

        Attribute records = attributes.get(recordType);
        return records != null && records.size() > 0;
    }
}
