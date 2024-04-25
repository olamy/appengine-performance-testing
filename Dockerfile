FROM ubuntu:22.04

# Update the system, install OpenSSH Server, and set up users
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y openssh-server
RUN addgroup -gid 1000 jenkins
# Create user and set password for user and root user
RUN  useradd -rm -d /home/jenkins -s /bin/bash -g jenkins -G sudo -u 1000 jenkins && \
    echo 'jenkins:secret_password' | chpasswd && \
    echo 'root:secret_password' | chpasswd

RUN echo "export VISIBLE=now" >> /etc/profile

# Set up configuration for SSH
RUN mkdir /var/run/sshd && \
    sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#LogLevel INFO/LogLevel VERBOSE/' /etc/ssh/sshd_config && \
    sed -i 's/#SyslogFacility AUTH/SyslogFacility AUTH/' /etc/ssh/sshd_config && \
    echo "SyslogFacility AUTHPRIV" >> /etc/ssh/sshd_config
    #sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd && \

RUN mkdir -p /home/jenkins/.ssh 
#COPY authorized_keys /home/jenkins/.ssh/authorized_keys
ADD --chown=jenkins:jenkins authorized_keys /home/jenkins/.ssh
RUN chown -R jenkins:jenkins /home/jenkins/.ssh
RUN chmod 700 /home/jenkins/.ssh && \
    chmod 600 /home/jenkins/.ssh/authorized_keys

COPY --from=eclipse-temurin:21.0.3_9-jdk /opt/java/openjdk /usr/local/java

# Expose the SSH port
EXPOSE 22

# Run SSH
CMD ["/usr/sbin/sshd", "-D"]
