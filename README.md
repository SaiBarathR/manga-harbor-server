# Manga Harbour - Manga Downloader Service

Manga Harbour is a Spring Boot application that provides a manga downloading service. It allows users to download manga volumes and chapters as ZIP files. This application integrates with the MangaDex API to fetch manga details and images.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Usage](#usage)
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
   cd manga-harbour
   ```

3. **Build and Run the Application:**

   ```bash
   mvn spring-boot:run
   ```

   The application will start running at `http://localhost:9000`.

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

## Contributing

Contributions are welcome! If you have any feature requests, bug reports, or suggestions, please open an issue on GitHub. Pull requests are also encouraged.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

Feel free to customize this README further to include additional sections like deployment instructions, troubleshooting, or specific API documentation if needed. Good luck with your project!
