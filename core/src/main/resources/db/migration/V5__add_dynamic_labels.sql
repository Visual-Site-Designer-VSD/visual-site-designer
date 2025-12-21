-- ========================================
-- Label Component Plugin
-- Migration V5: Add dynamic labels table and sample data
-- ========================================

-- ========================================
-- Table: plugin_label_dynamic_content
-- Stores dynamic content for Label components
-- ========================================
CREATE TABLE IF NOT EXISTS plugin_label_dynamic_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(100) NOT NULL DEFAULT 'label-component-plugin',
    content_key VARCHAR(100) NOT NULL,
    content TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    title VARCHAR(200),
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_label_key_lang UNIQUE (content_key, language, plugin_id)
);

CREATE INDEX IF NOT EXISTS idx_label_key_lang ON plugin_label_dynamic_content(content_key, language);
CREATE INDEX IF NOT EXISTS idx_label_plugin ON plugin_label_dynamic_content(plugin_id);
CREATE INDEX IF NOT EXISTS idx_label_active ON plugin_label_dynamic_content(is_active);

-- ========================================
-- Sample Data: About Me section
-- ========================================
INSERT INTO plugin_label_dynamic_content (plugin_id, content_key, content, language, title, description, is_active) VALUES
('label-component-plugin', 'about-me',
'Hi, I''m a passionate software developer with expertise in building modern web applications. I specialize in Java, Spring Boot, React, and TypeScript. With years of experience in full-stack development, I create scalable and maintainable solutions that solve real-world problems.

I believe in writing clean, testable code and following best practices. When I''m not coding, you can find me exploring new technologies, contributing to open-source projects, or sharing knowledge with the developer community.',
'en', 'About Me', 'Personal introduction for the About Me section', TRUE);

-- ========================================
-- Sample Data: Welcome Message
-- ========================================
INSERT INTO plugin_label_dynamic_content (plugin_id, content_key, content, language, title, description, is_active) VALUES
('label-component-plugin', 'welcome-message',
'Welcome to my portfolio! I''m glad you''re here. Take a look around to see my latest projects and learn more about what I do.',
'en', 'Welcome Message', 'Homepage welcome greeting', TRUE);

-- ========================================
-- Sample Data: Contact Info
-- ========================================
INSERT INTO plugin_label_dynamic_content (plugin_id, content_key, content, language, title, description, is_active) VALUES
('label-component-plugin', 'contact-info',
'Feel free to reach out! I''m always open to discussing new projects, creative ideas, or opportunities to be part of your vision.',
'en', 'Contact Info', 'Contact section introduction', TRUE);

-- ========================================
-- Sample Data: Skills Section
-- ========================================
INSERT INTO plugin_label_dynamic_content (plugin_id, content_key, content, language, title, description, is_active) VALUES
('label-component-plugin', 'skills-intro',
'I work with a variety of modern technologies to build robust and scalable applications. Here are some of the tools and frameworks I use regularly.',
'en', 'Skills Introduction', 'Introduction to skills section', TRUE);

-- ========================================
-- Sample Data: Footer Copyright
-- ========================================
INSERT INTO plugin_label_dynamic_content (plugin_id, content_key, content, language, title, description, is_active) VALUES
('label-component-plugin', 'footer-copyright',
'2024 All rights reserved. Built with passion and modern web technologies.',
'en', 'Footer Copyright', 'Footer copyright text', TRUE);
