
package com.verify_x.services;

import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class EmailDomainValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9-]+"
                    + "(\\.[A-Z0-9-]+)+$",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isValidFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalized = email.trim();

        if (normalized.length() > 254
                || !EMAIL_PATTERN.matcher(normalized).matches()) {
            return false;
        }

        String[] parts = normalized.split("@", -1);

        if (parts.length != 2
                || parts[0].length() > 64
                || parts[0].startsWith(".")
                || parts[0].endsWith(".")
                || parts[0].contains("..")) {
            return false;
        }

        String domain = parts[1];

        if (domain.length() > 253) {
            return false;
        }

        for (String label : domain.split("\\.")) {
            if (label.isEmpty()
                    || label.length() > 63
                    || label.startsWith("-")
                    || label.endsWith("-")) {
                return false;
            }
        }

        return true;
    }

    public boolean hasMailDomain(String email) {
        if (!isValidFormat(email)) {
            return false;
        }

        String domain = email.trim()
                .substring(email.trim().lastIndexOf('@') + 1)
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

                // RFC-compatible implicit MX fallback.
                return hasRecord(context, domain, "A")
                        || hasRecord(context, domain, "AAAA");
            } finally {
                context.close();
            }
        } catch (NamingException ex) {
            // A DNS outage is not proof of an invalid email.
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

        Attributes attributes = context.getAttributes(
                domain, new String[]{recordType}
        );

        Attribute records = attributes.get(recordType);
        return records != null && records.size() > 0;
    }
}
