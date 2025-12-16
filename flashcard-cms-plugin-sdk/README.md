# Flashcard CMS Plugin SDK

The Flashcard CMS Plugin SDK provides the core framework for building modular plugins for the Flashcard CMS platform. This SDK enables developers to extend the platform's functionality through a well-defined plugin architecture.

## Overview

The Plugin SDK provides:
- Plugin lifecycle management (load, activate, deactivate, uninstall)
- Base classes for entities, services, and controllers
- Annotations for plugin component discovery
- Access to platform services through PluginContext
- Data isolation and multi-tenancy support

## Architecture

### Core Components

#### 1. Plugin Interface
The main interface that all plugins must implement:

```java
public interface Plugin {
    String getPluginId();
    String getVersion();
    String getName();
    String getDescription();

    void onLoad(PluginContext context) throws Exception;
    void onActivate(PluginContext context) throws Exception;
    void onDeactivate(PluginContext context) throws Exception;
    void onUninstall(PluginContext context) throws Exception;
}
```

#### 2. PluginContext
Provides access to platform services and resources:

```java
public interface PluginContext {
    String getPluginId();
    Path getDataDirectory();
    Path getConfigDirectory();
    Logger getLogger();
    ApplicationContext getApplicationContext();
    ApplicationContext getPlatformContext();
    String getConfig(String key);
    void setConfig(String key, String value);
    ClassLoader getPluginClassLoader();
    boolean isActive();
    String getVersion();
}
```

#### 3. Base Classes

**PluginEntity**
- Base class for all plugin entities
- Provides common fields: `pluginId`, `createdAt`, `updatedAt`
- Ensures data isolation between plugins

**PluginService**
- Base class for plugin services
- Provides logging methods: `logInfo()`, `logDebug()`, `logError()`, `logWarn()`

**PluginController**
- Base class for plugin REST controllers
- Provides logging methods for HTTP request handling

#### 4. Annotations

- `@PluginEntity` - Marks entity classes for automatic discovery
- `@PluginService` - Marks service classes as plugin-managed Spring beans
- `@PluginController` - Marks REST controllers as plugin components

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Spring Boot 4.0.0 knowledge

### Adding SDK to Your Project

Add the SDK dependency to your plugin's `pom.xml`:

```xml
<dependency>
    <groupId>com.flashcard</groupId>
    <artifactId>flashcard-cms-plugin-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Creating a Plugin

#### Step 1: Create Plugin Main Class

```java
package com.example.myplugin;

import dev.mainul35.cms.sdk.Plugin;
import dev.mainul35.cms.sdk.PluginContext;
import org.slf4j.Logger;

public class MyPlugin implements Plugin {

    private static final String PLUGIN_ID = "my-plugin";
    private static final String VERSION = "1.0.0";

    private PluginContext context;
    private Logger logger;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getName() {
        return "My Custom Plugin";
    }

    @Override
    public String getDescription() {
        return "A custom plugin for the Flashcard CMS";
    }

    @Override
    public void onLoad(PluginContext context) throws Exception {
        this.context = context;
        this.logger = context.getLogger();
        logger.info("Loading {} v{}", getName(), VERSION);

        // Initialize resources
        logger.info("{} loaded successfully", getName());
    }

    @Override
    public void onActivate(PluginContext context) throws Exception {
        logger.info("Activating {}", getName());
        // Register controllers, start services
        logger.info("{} activated successfully", getName());
    }

    @Override
    public void onDeactivate(PluginContext context) throws Exception {
        logger.info("Deactivating {}", getName());
        // Cleanup, unregister controllers
        logger.info("{} deactivated successfully", getName());
    }

    @Override
    public void onUninstall(PluginContext context) throws Exception {
        logger.info("Uninstalling {}", getName());
        // Final cleanup
        logger.info("{} uninstalled successfully", getName());
    }
}
```

#### Step 2: Create Entity

```java
package com.example.myplugin.entity;

import dev.mainul35.cms.sdk.annotation.PluginEntity;
import dev.mainul35.cms.sdk.entity.PluginEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plugin_my_items")
@PluginEntity(pluginId = "my-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MyItem extends PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    public MyItem(String pluginId) {
        super(pluginId);
    }
}
```

#### Step 3: Create Repository

```java
package com.example.myplugin.repository;

import com.example.myplugin.entity.MyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MyItemRepository extends JpaRepository<MyItem, Long> {
    List<MyItem> findByNameContainingIgnoreCase(String keyword);
}
```

#### Step 4: Create Service

```java
package com.example.myplugin.service;

import com.example.myplugin.entity.MyItem;
import com.example.myplugin.repository.MyItemRepository;
import dev.mainul35.cms.sdk.annotation.PluginService;
import dev.mainul35.cms.sdk.service.PluginService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@PluginService(pluginId = "my-plugin")
@RequiredArgsConstructor
public class MyItemService extends PluginService {

    private final MyItemRepository repository;

    public List<MyItem> getAllItems() {
        logDebug("Fetching all items");
        return repository.findAll();
    }

    public Optional<MyItem> getItemById(Long id) {
        logDebug("Fetching item with id: {}", id);
        return repository.findById(id);
    }

    @Transactional
    public MyItem createItem(MyItem item) {
        logDebug("Creating item: {}", item.getName());

        if (item.getPluginId() == null) {
            item.setPluginId("my-plugin");
        }

        MyItem saved = repository.save(item);
        logInfo("Item created with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteItem(Long id) {
        logDebug("Deleting item with id: {}", id);
        repository.deleteById(id);
        logInfo("Item deleted: {}", id);
    }
}
```

#### Step 5: Create Controller

```java
package com.example.myplugin.controller;

import com.example.myplugin.entity.MyItem;
import com.example.myplugin.service.MyItemService;
import dev.mainul35.cms.sdk.annotation.PluginController;
import dev.mainul35.cms.sdk.controller.PluginController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PluginController(pluginId = "my-plugin", basePath = "/api/items")
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin
public class MyItemController extends PluginController {

    private final MyItemService service;

    @GetMapping
    public ResponseEntity<List<MyItem>> getAllItems() {
        try {
            return ResponseEntity.ok(service.getAllItems());
        } catch (Exception e) {
            logError("Error fetching items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyItem> getItemById(@PathVariable Long id) {
        try {
            return service.getItemById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logError("Error fetching item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<MyItem> createItem(@RequestBody MyItem item) {
        try {
            MyItem created = service.createItem(item);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logError("Error creating item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            service.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logError("Error deleting item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

## Best Practices

### 1. Entity Design
- Always extend `PluginEntity` base class
- Use `@PluginEntity` annotation with your plugin ID
- Prefix table names with `plugin_` to avoid conflicts
- Include plugin ID in custom constructors

### 2. Service Layer
- Extend `PluginService` for built-in logging
- Use `@PluginService` annotation
- Mark transaction boundaries with `@Transactional`
- Use logging methods: `logInfo()`, `logDebug()`, `logError()`, `logWarn()`

### 3. Controller Layer
- Extend `PluginController` for logging utilities
- Use `@PluginController` annotation with plugin ID and base path
- Handle exceptions gracefully
- Return appropriate HTTP status codes

### 4. Plugin Lifecycle
- Initialize resources in `onLoad()`
- Start services in `onActivate()`
- Stop services in `onDeactivate()`
- Clean up in `onUninstall()`

### 5. Logging
- Always use the logger from PluginContext
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Include contextual information in log messages

### 6. Configuration
- Store plugin-specific config using `context.setConfig()`
- Retrieve config using `context.getConfig()`
- Use plugin data directory for files: `context.getDataDirectory()`

## Plugin Structure

Recommended plugin project structure:

```
my-plugin/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── myplugin/
                        ├── MyPlugin.java           # Main plugin class
                        ├── entity/                 # JPA entities
                        │   └── MyItem.java
                        ├── repository/             # Spring Data repositories
                        │   └── MyItemRepository.java
                        ├── service/                # Business logic
                        │   └── MyItemService.java
                        └── controller/             # REST controllers
                            └── MyItemController.java
```

## Building the SDK

To build the SDK locally:

```bash
cd flashcard-cms-plugin-sdk
mvn clean install
```

This will install the SDK to your local Maven repository for use in plugin projects.

## API Reference

### Plugin Lifecycle Methods

| Method | Description | When Called |
|--------|-------------|-------------|
| `onLoad()` | Initialize plugin resources | Plugin installation or platform startup |
| `onActivate()` | Start plugin services | Plugin activation |
| `onDeactivate()` | Stop plugin services | Plugin deactivation |
| `onUninstall()` | Final cleanup | Plugin uninstallation |

### PluginContext Methods

| Method | Description |
|--------|-------------|
| `getPluginId()` | Get plugin identifier |
| `getDataDirectory()` | Get plugin data storage directory |
| `getConfigDirectory()` | Get plugin config directory |
| `getLogger()` | Get plugin-specific logger |
| `getApplicationContext()` | Get plugin Spring context |
| `getPlatformContext()` | Get platform Spring context |
| `getConfig(key)` | Get configuration value |
| `setConfig(key, value)` | Set configuration value |
| `getPluginClassLoader()` | Get plugin class loader |
| `isActive()` | Check if plugin is active |

### Logging Methods (PluginService & PluginController)

| Method | Description |
|--------|-------------|
| `logInfo(message, args...)` | Log info level message |
| `logDebug(message, args...)` | Log debug level message |
| `logWarn(message, args...)` | Log warning message |
| `logError(message, args...)` | Log error message |
| `logError(message, throwable, args...)` | Log error with exception |

## Dependencies

The SDK provides these dependencies (scope: provided):

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Hibernate Core
- Lombok
- SLF4J

These are marked as `provided` scope, meaning they will be supplied by the platform at runtime.

## Version

Current Version: 1.0.0-SNAPSHOT

## License

Part of the Flashcard CMS platform.

## Support

For plugin development support and examples, see the bundled plugins:
- `course-plugin` - Course and module management
- `lesson-plugin` - Lesson and media management
- `flashcard-plugin` - Flashcard and deck management
