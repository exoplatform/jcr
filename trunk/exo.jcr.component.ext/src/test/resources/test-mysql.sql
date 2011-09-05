CREATE DATABASE ${database};
USE ${database};
CREATE USER '${username}' IDENTIFIED BY '${password}';
GRANT SELECT,INSERT,UPDATE,DELETE ON ${database}.* TO '${username}';