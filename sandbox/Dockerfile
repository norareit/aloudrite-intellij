ARG AGENTRITE_IMAGE
FROM ${AGENTRITE_IMAGE}

# Amazon Corretto 25, the JDK the developer's host builds with. The folder in Corretto's image is named after the
# machine, x86_64 or aarch64, hence the wildcard.
COPY --from=amazoncorretto:25-jdk /usr/lib/jvm/java-25-amazon-corretto.* /opt/corretto-25/
# The JDK's lib/security/cacerts is a link to /etc/pki/java/cacerts, Amazon Linux's trust store, which bookworm does
# not have. Without it the JDK trusts no certificate, and the Gradle wrapper fails at its first download.
COPY --from=amazoncorretto:25-jdk /etc/pki/ca-trust/extracted/java/cacerts /etc/pki/java/cacerts
ENV JAVA_HOME=/opt/corretto-25
# The kit's entrypoint sets the PATH anew before it drops to the agent, so the JDK's programs are linked into a
# directory on that PATH rather than added to it here.
RUN ln -s /opt/corretto-25/bin/* /usr/local/bin/
