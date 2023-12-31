# Manga Harbour - Manga Downloader Service

Manga Harbour is a Spring Boot application that provides a manga downloading service. It allows users to download manga volumes and chapters as ZIP files. This application integrates with the MangaDex API to fetch manga details and images.

For Client Repo checkout: https://github.com/SaiBarathR/manga-harbor

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Getting Started with Docker](#getting-started-with-docker)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Integration with Manga Harbor Client](#integration-with-manga-harbor-client)
- [Contributing](#contributing)
- [License](#license)

## Features

- Download manga volumes and chapters as ZIP files.
- Fetch manga details, including title, cover image, and metadata, from MangaDex API.
- Support for downloading manga by volume or by chapter.
- Asynchronous processing for fetching image URLs to optimize performance.
- Error handling and logging for a seamless user experience.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Apache Maven
- Git
- MangaDex API Credentials (Get your API credentials from [MangaDex](https://api.mangadex.org/docs))

## Getting Started

1. **Clone the repository:**

   ```bash
   git clone https://github.com/SaiBarathR/manga-harbor-server.git
   cd manga-harbor-server
   ```

2. **Build the application:**

   ```bash
   mvn install
   ```

3. **Run the application:**

   ```bash
   mvn spring-boot:run
   ```

   The application will start running at `http://localhost:9000`.   
   
## Getting Started with Docker

1. **Clone the repository:**

   ```bash
   git clone
   cd manga-harbor-server
   ```

2. **Build the Docker Image:**

   ```bash
   docker build -t manga-harbor-server .
   ```

3. **Run the Docker Container:**

   ```bash
   docker run -p 9000:9000 manga-harbor-server
   ```

   The application will start running at `http://localhost:9000`.

## Configuration

The application can be configured by modifying the `application.properties` file. The following properties are available:

- **Server Port:**

  ```properties
  server.port=9000
  ```

  The port on which the application will run.

  - **Logging:**
  
  ```properties
   logging.level.com.logging=TRACE
   logging.file.name=error.log
   logging.pattern.file=%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){green} [%level] %c{1.} [%t] %m%n
   ```

   The logging level and file name can be configured here. The default logging level is `INFO`. The default log file is `error.log`.

- **Maximum In-Memory Size:**

  ```properties
  spring.codec.max-in-memory-size=80512KB
  ```

  The maximum size of the in-memory buffer for storing manga images. The default value is `80512KB`.


System properties can also be configured by modifying the `system.properties` file. The following properties are available:

- **Java Runtime Version:**

  ```properties
  java.runtime.version=17
  ```

  The Java runtime version used by the application. The default value is `17`.


## API Endpoints

- **Get Manga Details:**

  ```http
  GET /manga/{id}
  ```

  Get detailed information about a specific manga by its ID.

- **Search Manga:**

  ```http
  GET /manga/search/{title}
  ```

  Search for manga by title.

- **Download Manga:**

  ```http
  GET /manga/download/{mangaId}
  GET /manga/download/{mangaId}/{volume}
  GET /manga/download/{mangaId}/{volume}/{chapter}
  ```

  Download manga volumes or chapters as ZIP files.

## Usage

1. **Fetch Manga Details:**

   ```http
   GET http://localhost:9000/manga/{id}
   ```

   This endpoint retrieves detailed information about the manga with the specified ID.

2. **Search Manga:**

   ```http
   GET http://localhost:9000/manga/search/{title}
   ```

   Search for manga by its title.

3. **Download Manga:**

   ```http
   GET http://localhost:9000/manga/download/{mangaId}
   GET http://localhost:9000/manga/download/{mangaId}/{volume}
   GET http://localhost:9000/manga/download/{mangaId}/{volume}/{chapter}
   ```

   Download manga volumes or chapters as ZIP files.

## Integration with Manga Harbor Client

Manga Harbour Server seamlessly integrates with the Manga Harbor Client, providing the backend service for manga downloading functionality. To run the complete Manga Harbor application, follow these steps:

1. **Clone the Manga Harbor Client repository:**

   ```bash
   git clone https://github.com/SaiBarathR/manga-harbor.git
   cd manga-harbor
   ```

2. **Set Up the Manga Harbor React Client (Refer to [manga-harbor  README](https://github.com/SaiBarathR/manga-harbor#steps-to-run-the-react-app-with-manga-harbor-spring-server) for instructions).**

3. **Configure API Endpoint in Manga Harbor Client (if necessary):**

   If the Manga Harbor Server is running on a different port or host, update the API endpoint in the Manga Harbor Client. Open the `src/config/config.json` file and modify the `baseUrl` accordingly:

   ```json
   {
     "urls": {
       "manga": "manga/",
       "tags": "manga/tag",
       "grpMangaStats": "statistics/manga",
       "search": "manga/search/",
       "cover": "manga/cover?url=",
       "download": "manga/download/",
       "volumes": "manga/volumeList/"
     },
     "baseUrl": {
       "springBoot": "http://localhost:9000/",
       "mangaDex": "https://api.mangadex.org/"
     }
   }
   ```

   Replace `"http://localhost:9000/"` with the appropriate base URL of your Manga Harbor Spring server.

4. **Run the Manga Harbor Client:**

   ```bash
   npm install
   npm start
   ```

   The Manga Harbor Client will be accessible at `http://localhost:3000/`.

## Contributing

Contributions are welcome! If you have any feature requests, bug reports, or suggestions, please open an issue on GitHub. Pull requests are also encouraged.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

Feel free to customize this README further to include additional sections like deployment instructions, troubleshooting, or specific API documentation if needed. Good luck with your project!
```

This README now includes a section on how to integrate the `manga-harbor-server` with the `manga-harbor` client and provides instructions for running both applications together. Feel free to customize it further if needed!
