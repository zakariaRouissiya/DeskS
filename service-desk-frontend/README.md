# Service Desk Frontend

This project is an Angular application that serves as the frontend for the Service Desk application. It is designed to interact with a Spring Boot backend, providing a user-friendly interface for managing users and departments.

## Project Structure

- **src/app/components**: Contains reusable UI components for the application.
- **src/app/services**: Contains services that handle business logic and API calls to the Spring Boot backend.
- **src/app/models**: Contains TypeScript interfaces and classes that define the data models used in the application.
- **src/app/pages**: Contains components that represent different pages of the application, such as user management and department management.
- **src/app/app.module.ts**: The root module of the Angular application, importing necessary modules and setting up dependency injection.
- **src/assets**: Contains static assets such as images, styles, and other files.
- **src/environments**: Contains environment configuration files for different deployment scenarios.
- **src/index.html**: The main HTML file that serves as the entry point for the Angular application.
- **angular.json**: Configuration file for Angular CLI, specifying project structure and build options.
- **package.json**: Configuration file for npm, listing dependencies and scripts.
- **tsconfig.json**: Configuration file for TypeScript, specifying compiler options.

## Getting Started

To get started with the project, follow these steps:

1. Clone the repository:
   ```
   git clone <repository-url>
   ```

2. Navigate to the project directory:
   ```
   cd service-desk-frontend
   ```

3. Install the dependencies:
   ```
   npm install
   ```

4. Run the application:
   ```
   ng serve
   ```

5. Open your browser and navigate to `http://localhost:4200`.

## API Integration

This application communicates with the Spring Boot backend. Ensure that the backend is running and accessible at the configured API endpoints in the environment files.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the LICENSE file for details.