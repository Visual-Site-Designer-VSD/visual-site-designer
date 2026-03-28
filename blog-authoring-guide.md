# Blog Authoring Guide — VSD CMS

Build a fully functional blog application using drag-and-drop in VSD CMS, then export it as a Spring Boot + React SPA.

---

## Prerequisites

- VSD CMS running locally
- The following plugins installed via Plugin Manager:
  - `auth-context-plugin`
  - `auth-component-plugin`
  - `navbar-component-plugin`
  - `user-profile-plugin`
  - `article-list-plugin`
  - `article-viewer-plugin`
  - `article-editor-plugin`
  - `page-layout-plugin`
  - `container-layout-plugin`

---

## Step 1 — Create the Home Page (Article Listing)

1. Open VSD CMS Builder
2. Create a new page: **Name** = `Home`, **Slug** = `home`
3. Drag **PageLayout** onto the canvas (this gives you header/center/footer regions)
4. **Header region** — drag in:
   - **NavbarDefault** — set brand text to your blog name (e.g., "My Dev Blog")
   - Edit nav items JSON to include your pages:
     ```json
     [
       { "label": "Home", "href": "/" },
       { "label": "Write", "href": "/write" },
       { "label": "Login", "href": "/login" }
     ]
     ```
   - **UserProfileBadge** — drop it inside the navbar area (right side). Configure:
     - Variant: `compact`
     - Show Avatar: `true`
     - Show Name: `true`
     - Login URL: `/login`
5. **Center region** — drag in:
   - **ArticleList** — configure in the properties panel:
     - API Endpoint: `/api/proxy/articles`
     - Layout: `cards`
     - Grid Columns: `3`
     - Page Size: `9`
     - Article Link Pattern: `/article/{id}`
     - Show Author: `true`
     - Show Date: `true`
     - Show Tags: `true`
     - Show Cover Image: `true`
     - Show Pagination: `true`
6. **Save** the page

---

## Step 2 — Create the Article Page (Single Article View)

1. Create a new page: **Name** = `Article`, **Slug** = `article`
2. Drag **PageLayout** onto the canvas
3. **Header region** — reuse the same Navbar + UserProfileBadge setup from Step 1
4. **Center region** — drag in:
   - **ArticleViewer** — configure:
     - API Endpoint: `/api/proxy/articles`
     - Back Link Text: `Back to articles`
     - Back Link URL: `/`
     - Show Author: `true`
     - Show Date: `true`
     - Show Tags: `true`
     - Show Cover Image: `true`
5. **Save** the page

> The ArticleViewer reads the article ID from the URL automatically.
> When a user clicks an article card on the home page (`/article/123`), the viewer fetches and renders it.

---

## Step 3 — Create the Write Page (Article Editor)

1. Create a new page: **Name** = `Write`, **Slug** = `write`
2. Drag **PageLayout** onto the canvas
3. **Header region** — same Navbar + UserProfileBadge
4. **Center region** — drag in:
   - **ArticleEditor** — configure:
     - API Endpoint: `/api/proxy/articles`
     - Show Title Field: `true`
     - Show Cover Image Field: `true`
     - Show Tags Field: `true`
     - Show Toolbar: `true`
     - Show Save Draft: `true`
     - Show Publish: `true`
     - Redirect After Publish: `/`
     - Login Prompt Text: `Please sign in to write articles.`
     - Login URL: `/login`
5. **Save** the page

> The editor is protected by auth context. Unauthenticated visitors see a "Please sign in" prompt instead of the editor.

### What the Editor Supports

| Feature | How to Use |
|---------|-----------|
| **Bold** | Toolbar `B` button or `Ctrl+B` |
| **Italic** | Toolbar `I` button or `Ctrl+I` |
| **Headings** | Toolbar `H1`, `H2`, `H3` buttons |
| **Bullet list** | Toolbar bullet button |
| **Ordered list** | Toolbar numbered list button |
| **Blockquote** | Toolbar quote button |
| **Code block** | Toolbar `</>` button — paste code inside |
| **Images** | Toolbar camera button — enter image URL |
| **Links** | Select text, click link button, enter URL |
| **Horizontal rule** | Toolbar dash button |
| **Undo/Redo** | Toolbar arrows or `Ctrl+Z` / `Ctrl+Shift+Z` |

---

## Step 4 — Create the Login Page

1. Create a new page: **Name** = `Login`, **Slug** = `login`
2. Drag **PageLayout** onto the canvas
3. **Header region** — same Navbar (without UserProfileBadge, or keep it — it shows "Sign In" link when logged out)
4. **Center region** — drag in:
   - **SocialLoginButtons** (from auth-component-plugin) — configure:
     - Show Google: `true`
     - Show GitHub: `true`
     - Layout: `vertical`
     - Button Style: `filled`

   OR use **LoginForm** if your auth server supports username/password.

5. **Save** the page

---

## Step 5 — Export as Spring Boot + React SPA

1. Click the **Export** button in the toolbar
2. Select **Spring Boot + React SPA** (the recommended option)
3. Fill in project configuration:
   - Project Name: `my-blog`
   - Group ID: `com.example`
   - Artifact ID: `my-blog`
   - Spring Boot Version: `3.2.0`
   - Java Version: `21`
   - **Authentication: `Social Login` or `SSO`** (must match your auth server)
4. Click **Download SPA Project**
5. Extract the ZIP file

---

## Step 6 — Configure the Exported Project

Open `src/main/resources/application.properties` in the exported project:

```properties
# ---- API Gateway (your article backend) ----
site.runtime.api.gateway-url=http://localhost:8081

# ---- Authentication ----
site.runtime.auth.type=social

# Google OAuth2
site.runtime.auth.social.google.enabled=true
site.runtime.auth.social.google.client-id=YOUR_GOOGLE_CLIENT_ID
site.runtime.auth.social.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# GitHub OAuth2
site.runtime.auth.social.github.enabled=true
site.runtime.auth.social.github.client-id=YOUR_GITHUB_CLIENT_ID
site.runtime.auth.social.github.client-secret=YOUR_GITHUB_CLIENT_SECRET

# Where to redirect after login
site.runtime.auth.login-success-url=/
```

> **`gateway-url`** is the base URL of your article microservice.
> The proxy controller forwards:
> - `GET /api/proxy/articles` → `GET {gateway-url}/articles` (public, no token)
> - `POST /api/proxy/articles` → `POST {gateway-url}/articles` (with OAuth2 token)

---

## Step 7 — Run the Blog

```bash
cd my-blog
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

### What You Should See

| Page | URL | Behavior |
|------|-----|----------|
| Home | `/` | Grid of article cards fetched from your API |
| Article | `/article/{id}` | Full article with cover image, author, tags, content |
| Write | `/write` | "Sign in" prompt (if not logged in) or TipTap editor |
| Login | `/login` | Social login buttons (Google, GitHub) |

---

## Step 8 — Write Your First Article

1. Go to `/login` and sign in with Google or GitHub
2. The navbar should now show your avatar and name (UserProfileBadge)
3. Go to `/write`
4. Fill in:
   - **Title** — your article title
   - **Cover Image URL** — paste a URL to a cover image (optional)
   - **Content** — write using the TipTap editor:
     - Type text normally for paragraphs
     - Use `H2` for section headings
     - Click the code button (`</>`) for code blocks
     - Click the image button to embed images by URL
     - Select text and click the link button to add hyperlinks
   - **Tags** — comma-separated (e.g., `java, spring-boot, tutorial`)
5. Click **Publish** to publish, or **Save Draft** to save without publishing
6. You will be redirected to the home page where your article appears in the list

---

## Article API Contract

Your article backend must implement these endpoints:

### GET /articles?page=0&size=10

Returns paginated list:

```json
{
  "content": [
    {
      "id": "1",
      "title": "My First Post",
      "summary": "A brief intro...",
      "coverImageUrl": "https://...",
      "author": { "id": "u1", "name": "Jane", "avatarUrl": "https://..." },
      "tags": ["java", "spring"],
      "publishedAt": "2024-01-15T10:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "page": 0,
  "size": 10
}
```

### GET /articles/{id}

Returns single article:

```json
{
  "id": "1",
  "title": "My First Post",
  "content": "<h2>Introduction</h2><p>Hello world...</p>",
  "summary": "A brief intro...",
  "coverImageUrl": "https://...",
  "author": { "id": "u1", "name": "Jane", "avatarUrl": "https://..." },
  "tags": ["java", "spring"],
  "publishedAt": "2024-01-15T10:00:00Z"
}
```

### POST /articles

Accepts (requires Bearer token):

```json
{
  "title": "My First Post",
  "content": "<h2>Introduction</h2><p>Hello world...</p>",
  "summary": "A brief intro...",
  "coverImageUrl": "https://...",
  "tags": ["java", "spring"],
  "status": "published"
}
```

---

## Page Composition Summary

```
Home Page (/)
├── PageLayout
│   ├── Header
│   │   ├── NavbarDefault (brand + nav links)
│   │   └── UserProfileBadge (avatar/name or "Sign In")
│   └── Center
│       └── ArticleList (cards grid, paginated)

Article Page (/article/{id})
├── PageLayout
│   ├── Header
│   │   ├── NavbarDefault
│   │   └── UserProfileBadge
│   └── Center
│       └── ArticleViewer (full article with HTML content)

Write Page (/write)
├── PageLayout
│   ├── Header
│   │   ├── NavbarDefault
│   │   └── UserProfileBadge
│   └── Center
│       └── ArticleEditor (TipTap editor, auth-gated)

Login Page (/login)
├── PageLayout
│   ├── Header
│   │   └── NavbarDefault
│   └── Center
│       └── SocialLoginButtons (Google, GitHub)
```

---

## Docker Deployment

The exported project includes a `Dockerfile`. To build and run:

```bash
# Build the JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t my-blog .

# Run with environment variables
docker run -p 8080:8080 \
  -e SITE_RUNTIME_API_GATEWAY_URL=http://article-service:8081 \
  -e SITE_RUNTIME_AUTH_TYPE=social \
  -e SITE_RUNTIME_AUTH_SOCIAL_GOOGLE_ENABLED=true \
  -e SITE_RUNTIME_AUTH_SOCIAL_GOOGLE_CLIENT_ID=your-id \
  -e SITE_RUNTIME_AUTH_SOCIAL_GOOGLE_CLIENT_SECRET=your-secret \
  my-blog
```
