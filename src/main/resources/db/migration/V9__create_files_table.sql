
CREATE TABLE files (
id UUID PRIMARY KEY,
original_filename VARCHAR(255) NOT NULL,
storage_filename VARCHAR(100) NOT NULL UNIQUE,

 content_type VARCHAR(100) NOT NULL,
 file_size_bytes BIGINT NOT NULL,

 uploaded_by_account_type VARCHAR(20) NOT NULL,
 uploaded_by_account_id UUID NOT NULL,
 uploaded_by_email VARCHAR(150) NOT NULL,

created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

 CONSTRAINT chk_files_account_type
 CHECK (uploaded_by_account_type IN ('ADMIN', 'EMPLOYEE', 'USER')),

 CONSTRAINT chk_files_size
 CHECK (file_size_bytes > 0 AND file_size_bytes <= 5242880),

    CONSTRAINT chk_files_content_type
 CHECK (content_type IN (
 'application/pdf',
 'image/jpeg',
 'image/png'
                               ))
);

CREATE INDEX idx_files_uploaded_by
    ON files (uploaded_by_account_type, uploaded_by_account_id);

CREATE INDEX idx_files_created_at
    ON files (created_at);
