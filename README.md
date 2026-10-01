# Directory Tree

```tree
apex-backend
├─ .DS_Store
├─ .mvn
│  └─ wrapper
│     └─ maven-wrapper.properties
├─ README.md
├─ mvnw
├─ mvnw.cmd
├─ pom.xml
└─ src
   ├─ main
   │  ├─ java
   │  │  └─ com
   │  │     └─ apex
   │  │        ├─ Application.java
   │  │        ├─ config
   │  │        │  ├─ DataSeeder.java
   │  │        │  └─ SecurityConfig.java
   │  │        ├─ controller
   │  │        │  ├─ AuthController.java
   │  │        │  ├─ CategoryController.java
   │  │        │  ├─ HealthController.java
   │  │        │  └─ ProductController.java
   │  │        ├─ dto
   │  │        │  ├─ request
   │  │        │  │  ├─ AuthRequests.java
   │  │        │  │  ├─ CategoryRequests.java
   │  │        │  │  └─ ProductRequests.java
   │  │        │  └─ response
   │  │        │     ├─ AuthResponses.java
   │  │        │     ├─ CategoryResponse.java
   │  │        │     └─ ProductResponses.java
   │  │        ├─ exception
   │  │        │  ├─ DuplicateResourceException.java
   │  │        │  ├─ GlobalExceptionHandler.java
   │  │        │  ├─ ResourceNotFoundException.java
   │  │        │  └─ UnauthorizedException.java
   │  │        ├─ mapper
   │  │        │  ├─ CategoryMapper.java
   │  │        │  ├─ ProductMapper.java
   │  │        │  └─ UserMapper.java
   │  │        ├─ model
   │  │        │  ├─ Category.java
   │  │        │  ├─ MediaAsset.java
   │  │        │  ├─ Product.java
   │  │        │  ├─ ProductBarcode.java
   │  │        │  ├─ ProductVariant.java
   │  │        │  ├─ RefreshToken.java
   │  │        │  ├─ Role.java
   │  │        │  └─ User.java
   │  │        ├─ repository
   │  │        │  ├─ CategoryRepository.java
   │  │        │  ├─ MediaAssetRepository.java
   │  │        │  ├─ ProductBarcodeRepository.java
   │  │        │  ├─ ProductRepository.java
   │  │        │  ├─ ProductVariantRepository.java
   │  │        │  ├─ RefreshTokenRepository.java
   │  │        │  ├─ RoleRepository.java
   │  │        │  └─ UserRepository.java
   │  │        └─ service
   │  │           ├─ AuthService.java
   │  │           ├─ CategoryService.java
   │  │           ├─ JwtService.java
   │  │           └─ ProductService.java
   │  └─ resources
   │     ├─ application.properties
   │     ├─ static
   │     └─ templates
   └─ test
      └─ java
         └─ com
            └─ apex
               └─ app
                  └─ AppApplicationTests.java

```
