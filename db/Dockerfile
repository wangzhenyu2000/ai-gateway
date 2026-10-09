FROM mysql:8.4

# COPY lands as root:root 0644 inside the image, which MySQL accepts
# (a Windows bind-mount would be 0777 and silently ignored).
COPY db/conf/charset.cnf /etc/mysql/conf.d/charset.cnf
