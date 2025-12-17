# Course Plugin - Plugin Development Example

This is a bundled plugin for the Flashcard CMS platform that demonstrates how to build a complete plugin using the Plugin SDK. It provides course and module management functionality.

## Overview

The Course Plugin is a fully-featured example showing:
- Plugin lifecycle implementation
- Entity design with JPA relationships
- Service layer with business logic
- REST API controllers
- Data isolation using the Plugin SDK

## Features

- Create, read, update, and delete courses
- Manage modules within courses
- Search courses by title
- Eager and lazy loading of relationships
- Complete REST API endpoints

## Project Structure

```
course-plugin/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── dev/
                └── mainul35/
                    └── plugins/
                        └── course/
                            ├── CoursePlugin.java            # Main plugin class
                            ├── entity/                      # Domain entities
                            │   ├── Course.java
                            │   └── Module.java
                            ├── repository/                  # Data access
                            │   ├── CourseRepository.java
                            │   └── ModuleRepository.java
                            ├── service/                     # Business logic
                            │   ├── CourseService.java
                            │   └── ModuleService.java
                            └── controller/                  # REST API
                                ├── CourseController.java
                                └── ModuleController.java
```

## Development Guide

### Prerequisites

- Java 21
- Maven 3.8+
- Flashcard CMS Plugin SDK 1.0.0-SNAPSHOT
- Spring Boot 4.0.0

### Setting Up Development Environment

#### 1. Clone the Repository

```bash
cd flashcard-app
```

The course-plugin is located at `plugins/course-plugin/`

#### 2. Build the Plugin SDK First

The plugin depends on the SDK, so build it first:

```bash
cd flashcard-cms-plugin-sdk
mvn clean install
cd ..
```

#### 3. Build the Plugin

```bash
cd plugins/course-plugin
mvn clean install
```

### Development Workflow

#### Building the Plugin

```bash
# From the plugin directory
mvn clean package

# Or from the root directory (builds everything)
cd ../..
mvn clean package
```

#### Running the Application with the Plugin

The course-plugin is a bundled plugin and is automatically loaded when you run the main application:

```bash
# From the root directory
cd flashcard-app
mvn spring-boot:run
```

The application will:
1. Start on port 8080 (default)
2. Load the course-plugin automatically
3. Initialize database tables for Course and Module
4. Register REST endpoints under `/api/courses` and `/api/modules`

#### Verify Plugin is Running

Check the logs for:
```
Loading Course Management Plugin v1.0.0
Initializing course management resources
Course Management Plugin loaded successfully
Activating Course Management Plugin v1.0.0
Course Management Plugin activated successfully
```

### Understanding the Code

#### 1. Main Plugin Class

`CoursePlugin.java` implements the `Plugin` interface:

```java
public class CoursePlugin implements Plugin {
    @Override
    public void onLoad(PluginContext context) {
        // Initialize resources
        // Register entities
    }

    @Override
    public void onActivate(PluginContext context) {
        // Register controllers
        // Start services
    }

    @Override
    public void onDeactivate(PluginContext context) {
        // Stop services
        // Cleanup
    }

    @Override
    public void onUninstall(PluginContext context) {
        // Final cleanup
    }
}
```

#### 2. Entity Layer

**Course Entity** (`entity/Course.java`):
- Extends `PluginEntity` for common fields
- Uses `@PluginEntity` annotation
- Has one-to-many relationship with Module
- Table: `plugin_course_courses`

**Module Entity** (`entity/Module.java`):
- Extends `PluginEntity`
- Has many-to-one relationship with Course
- Table: `plugin_course_modules`

Key points:
- Always extend `PluginEntity` base class
- Use `@PluginEntity(pluginId = "course-plugin")` annotation
- Prefix table names with `plugin_`
- Include `pluginId` field for data isolation

#### 3. Repository Layer

**CourseRepository** (`repository/CourseRepository.java`):
```java
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByOrderByDisplayOrderAsc();
    List<Course> findByTitleContainingIgnoreCase(String keyword);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.modules WHERE c.id = :id")
    Optional<Course> findByIdWithModules(@Param("id") Long id);
}
```

Uses Spring Data JPA for automatic implementation. Custom query methods for:
- Ordering by display order
- Searching by title
- Eager loading modules

#### 4. Service Layer

**CourseService** (`service/CourseService.java`):
- Extends `PluginService` for logging utilities
- Uses `@PluginService(pluginId = "course-plugin")`
- Implements business logic
- Handles transactions with `@Transactional`
- Sets `pluginId` automatically

Example:
```java
@PluginService(pluginId = "course-plugin")
@RequiredArgsConstructor
public class CourseService extends PluginService {

    @Transactional
    public Course createCourse(Course course) {
        logDebug("Creating new course: {}", course.getTitle());

        if (course.getPluginId() == null) {
            course.setPluginId("course-plugin");
        }

        Course saved = courseRepository.save(course);
        logInfo("Course created with id: {}", saved.getId());
        return saved;
    }
}
```

#### 5. Controller Layer

**CourseController** (`controller/CourseController.java`):
- Extends `PluginController` for logging
- Uses `@PluginController(pluginId = "course-plugin", basePath = "/api/courses")`
- Defines REST endpoints
- Handles errors gracefully

Example endpoints:
```java
@PluginController(pluginId = "course-plugin", basePath = "/api/courses")
@RequestMapping("/api/courses")
public class CourseController extends PluginController {

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() { }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) { }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) { }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id) { }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) { }
}
```

## API Endpoints

### Course Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/courses` | Get all courses |
| GET | `/api/courses/{id}` | Get course by ID |
| GET | `/api/courses/{id}/with-modules` | Get course with modules |
| POST | `/api/courses` | Create new course |
| PUT | `/api/courses/{id}` | Update course |
| DELETE | `/api/courses/{id}` | Delete course |
| GET | `/api/courses/search?keyword={keyword}` | Search courses |

### Module Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/modules` | Get all modules |
| GET | `/api/modules/{id}` | Get module by ID |
| GET | `/api/modules/course/{courseId}` | Get modules by course |
| POST | `/api/modules` | Create new module |
| PUT | `/api/modules/{id}` | Update module |
| DELETE | `/api/modules/{id}` | Delete module |

### Example Requests

**Create a Course**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Java",
    "description": "Learn Java programming basics",
    "displayOrder": 1
  }'
```

**Get All Courses**:
```bash
curl http://localhost:8080/api/courses
```

**Create a Module**:
```bash
curl -X POST http://localhost:8080/api/modules \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Variables and Data Types",
    "description": "Understanding Java variables",
    "displayOrder": 1,
    "course": {
      "id": 1
    }
  }'
```

**Search Courses**:
```bash
curl "http://localhost:8080/api/courses/search?keyword=java"
```

## Testing

### Manual Testing with cURL

See the example requests above.

### Testing with Postman

Import this collection to test all endpoints:

1. Create a new Postman collection
2. Add requests for each endpoint
3. Set base URL to `http://localhost:8080`
4. Test CRUD operations

### Writing Unit Tests

Example unit test for CourseService:

```java
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void testCreateCourse() {
        Course course = new Course();
        course.setTitle("Test Course");

        when(courseRepository.save(any(Course.class)))
            .thenReturn(course);

        Course result = courseService.createCourse(course);

        assertNotNull(result);
        assertEquals("Test Course", result.getTitle());
        assertEquals("course-plugin", result.getPluginId());
    }
}
```

### Integration Testing

Example integration test:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CourseControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetAllCourses() {
        ResponseEntity<List> response = restTemplate
            .getForEntity("/api/courses", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

## Database Schema

### Courses Table

```sql
CREATE TABLE plugin_course_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### Modules Table

```sql
CREATE TABLE plugin_course_modules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    display_order INT DEFAULT 0,
    course_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES plugin_course_courses(id)
);
```

## Debugging

### Enable Debug Logging

Add to `application.properties`:

```properties
logging.level.dev.mainul35.plugins.course=DEBUG
```

### Common Issues

**Plugin not loading:**
- Check if the plugin JAR is in the correct location
- Verify plugin ID matches in all annotations
- Check logs for initialization errors

**Entities not persisting:**
- Verify `@PluginEntity` annotation is present
- Check that entity extends `PluginEntity`
- Ensure `pluginId` is set before saving

**Controllers not accessible:**
- Verify `@PluginController` annotation
- Check that controller extends `PluginController`
- Ensure Spring component scanning is configured

## Extending the Plugin

### Adding a New Entity

1. Create entity class extending `PluginEntity`
2. Add `@PluginEntity` annotation
3. Create corresponding repository
4. Create service class
5. Create controller class

### Adding Business Logic

1. Add methods to service class
2. Use logging methods from `PluginService`
3. Mark transactional methods with `@Transactional`

### Adding New Endpoints

1. Add methods to controller
2. Use appropriate HTTP method annotations
3. Handle exceptions properly
4. Log errors using `logError()`

## Best Practices Demonstrated

1. **Separation of Concerns**: Clear separation between entities, repositories, services, and controllers
2. **Logging**: Consistent use of SDK logging methods
3. **Error Handling**: Graceful error handling with appropriate HTTP status codes
4. **Transactions**: Proper transaction management for data modifications
5. **Data Isolation**: Using `pluginId` for multi-tenant data
6. **Relationships**: Proper JPA relationship mappings
7. **Lazy Loading**: Strategic use of lazy/eager loading

## Building for Production

```bash
# Build with production profile
mvn clean package -Pprod

# The plugin JAR will be in target/course-plugin-1.0.0-SNAPSHOT.jar
```

## Plugin Configuration

Configuration is stored per plugin instance. Example:

```java
@Override
public void onLoad(PluginContext context) {
    // Set default configuration
    if (context.getConfig("max.courses.per.user") == null) {
        context.setConfig("max.courses.per.user", "100");
    }

    // Read configuration
    String maxCourses = context.getConfig("max.courses.per.user", "50");
}
```

## Dependencies

Defined in `pom.xml`:

```xml
<dependencies>
    <!-- Plugin SDK -->
    <dependency>
        <groupId>com.flashcard</groupId>
        <artifactId>flashcard-cms-plugin-sdk</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Spring Boot dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## Version

Current Version: 1.0.0-SNAPSHOT

## Related Plugins

- **lesson-plugin**: Lesson and media management
- **flashcard-plugin**: Flashcard and deck management

## Resources

- [Plugin SDK Documentation](../flashcard-cms-plugin-sdk/README.md)
- Spring Boot Documentation
- Spring Data JPA Documentation

## Contributing

When contributing to this example plugin:
1. Follow the existing code structure
2. Use SDK base classes and annotations
3. Add appropriate logging
4. Handle errors gracefully
5. Write clear commit messages

## License

Part of the Flashcard CMS platform.
