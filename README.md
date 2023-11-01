## Spring Boot Vault Demo

### Install Postgres
Follow instructions 
For Ubuntu - https://www.postgresql.org/download/linux/ubuntu/
For Windows - https://www.postgresqltutorial.com/install-postgresql/

### Setup admin user in Postgres
```
postgres=# create user vaultech_admin password 'admin123';
CREATE ROLE
postgres=# ALTER USER vaultech_admin WITH SUPERUSER;
postgres=# \q
```

### Install Hashicorp Vault
```
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault --set server.dev.enabled=true

brew tap hashicorp/tap
brew install hashicorp/tap/vault

brew upgrade hashicorp/tap/vault

```

### Enable the database secrets engine
```
vault secrets enable database
```

### Configure PostgreSQL secrets engine
```
vault write database/config/postgresql \
     plugin_name=postgresql-database-plugin \
     connection_url="postgresql://{{username}}:{{password}}@localhost:5432/postgres?sslmode=disable" \
     allowed_roles="*" \
     username="vaultech_admin" \
     password="admin123"
```

### Verify the configuration
```
vault write database/roles/myrole db_name=postgresql \
     creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\"; GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
     default_ttl="2m" \
     max_ttl="4m"
```
