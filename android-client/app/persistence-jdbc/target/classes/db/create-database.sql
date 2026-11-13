-- Run ONCE as the MySQL root/admin user to create the SCASP databases and a local application user.
--   mysql -u root -p -e "source database/create-database.sql"
-- Change the password below to your own LOCAL value. Do not commit real passwords.
CREATE DATABASE IF NOT EXISTS scasp      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scasp_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'scasp'@'localhost' IDENTIFIED BY 'change_me_local_only';
CREATE USER IF NOT EXISTS 'scasp'@'127.0.0.1' IDENTIFIED BY 'change_me_local_only';
GRANT ALL PRIVILEGES ON scasp.*      TO 'scasp'@'localhost';
GRANT ALL PRIVILEGES ON scasp_test.* TO 'scasp'@'localhost';
GRANT ALL PRIVILEGES ON scasp.*      TO 'scasp'@'127.0.0.1';
GRANT ALL PRIVILEGES ON scasp_test.* TO 'scasp'@'127.0.0.1';
FLUSH PRIVILEGES;
