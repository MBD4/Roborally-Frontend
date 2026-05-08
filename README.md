Welcome to our final submission for Assignment 7 of the Advanced Programming course (02324) at DTU Compute.
We have built a complete, full-stack application designed to handle matchmaking and lobby coordination for the game RoboRally.  
To properly reflect modern web architecture, we divided our system into two entirely separate projects that communicate over a network:
a backend server and a frontend desktop client.


Features Implemented
We built several core features to make the multiplayer experience seamless:
- User Management:Players can register a new account or sign in using a username that is at least four characters long.
  The uniqueness of usernames, is checked automatically since we added @Column(unique=true) on the name attribute of the User class.
- Matchmaking & Lobbies: From the main dashboard, players can browse a live list of all open games, checking who owns them and how many slots are left.
  We can either join an existing game or create a brand new one.
- Ownership & Permissions: When we create a new game, we set the minimum and maximum number of players,
  and the system automatically makes us the game's "Owner," placing us into the first player slot.
- Lobby Flexibility: If we change our minds, we can easily leave a game we previously joined.
  If we are the owner of a game, we have the exclusive permission to delete the lobby entirely.
- Game Launch: Once a lobby reaches the required number of players, the game owner can officially start the match.
  Doing this updates the game's state to ACTIVE so no one else can join, and it immediately launches the interactive RoboRally grid board for the players to see.


Architecture: How It Works Under the Hood
The Backend (Server)
The backend project serves as the central brain of the system. It is a REST API built using the Java Spring Boot framework.
To keep our code clean, scalable, and easy to read, we structured the backend using a strict Controller → Service → Repository design pattern:
1. Controllers: Classes like GameController, UserController, and PlayerController define our REST API endpoints (handling GET, POST, DELETE, and PATCH requests).
   They receive the HTTP requests from the frontend and map them to the correct actions.
2. Services: The controllers don't handle the logic themselves; they pass data down to our service classes (GameService, UserService, PlayerService).
   This is where our core business logic lives. For example, when creating a game, GameService uses a @Transactional method to ensure the game is
   saved and the creator is assigned as the owner in the first player slot simultaneously.
3. Repositories: Finally, the services talk to the database using Spring Data REST Repositories (GameRepository, UserRepository, PlayerRepository).
   Here we also added some of our own methods using JPQL, to make our services able to call more methods than those that were available by default.


The Frontend (Client)
On the other side of the system is the frontend project, which is a JavaFX desktop application that players actually interact with.
The client uses a built-in REST client to communicate with our backend Controllers, fetching live updates about lobbies and posting user actions over HTTP.
When we click "Join" or "Refresh," the frontend sends the specific network request and updates the graphical user interface based on the server's response.
If multiple clients are running and a change is made on one client, it doesn't automatically update the others, so you need to hit the refresh button,
that sends a get request for the updated state of the DB. 
