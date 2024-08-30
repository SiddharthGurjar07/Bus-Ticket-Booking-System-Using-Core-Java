import java.sql.*;
import java.util.LinkedList;
import java.util.Scanner;

class Seat {
    private int seatNumber;
    private boolean isAvailable;

    public Seat(int seatNumber, boolean isAvailable) {
        this.seatNumber = seatNumber;
        this.isAvailable = isAvailable;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void book() {
        isAvailable = false;
    }
}

class User {
    private String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

class BusRoute {
    private String source;
    private String destination;
    private double adultFare;
    private Seat[][] seats;

    public BusRoute(String source, String destination, double adultFare) {
        this.source = source;
        this.destination = destination;
        this.adultFare = adultFare;
        seats = new Seat[7][5]; // 7 rows, 5 seats per row

        int seatNumber = 1;
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 5; col++) {
                seats[row][col] = new Seat(seatNumber++, true);
            }
        }
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getAdultFare() {
        return adultFare;
    }

    public Seat[][] getSeats() {
        return seats;
    }
}

public class BusTicketBookingSystem {
    private LinkedList<User> users;
    private LinkedList<BusRoute> routes;
    private User currentUser;
    private Scanner scanner;
    private Connection connection;

    public BusTicketBookingSystem() {
        this.users = new LinkedList<>();
        this.routes = new LinkedList<>();
        this.scanner = new Scanner(System.in);
        this.connection = createDatabaseConnection();
        createTables();
    }

    private Connection createDatabaseConnection() {
        Connection conn = null;
        try {
            String dburl = "jdbc:mysql://localhost:3306/Bus";
            String dbuser = "root";
            String dbpass = "";
            String drivername = "com.mysql.cj.jdbc.Driver";

            Class.forName(drivername);
            conn = DriverManager.getConnection(dburl, dbuser, dbpass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    private void createTables() {
        try {
            Statement statement = connection.createStatement();

            String createUserTable = "CREATE TABLE IF NOT EXISTS Users (Username VARCHAR(255) PRIMARY KEY, Password VARCHAR(255))";
            statement.executeUpdate(createUserTable);

            String createRouteTable = "CREATE TABLE IF NOT EXISTS Routes (RouteID INT AUTO_INCREMENT PRIMARY KEY, Source VARCHAR(255), Destination VARCHAR(255), AdultFare DOUBLE)";
            statement.executeUpdate(createRouteTable);

            String createBookingTable = "CREATE TABLE IF NOT EXISTS Bookings (BookingID INT AUTO_INCREMENT PRIMARY KEY, Username VARCHAR(255), Route VARCHAR(255), PassengerName VARCHAR(255), SeatNumber INT, TicketFare DOUBLE)";
            statement.executeUpdate(createBookingTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void registerUser(String username, String password) {
        try {
            PreparedStatement insertUser = connection
                    .prepareStatement("INSERT INTO Users (Username, Password) VALUES (?, ?)");
            insertUser.setString(1, username);
            insertUser.setString(2, password);
            insertUser.executeUpdate();
            System.out.println("Registration successful for user: " + username + "\n");
        } catch (SQLException e) {
            System.out.println("Registration failed. Username is already taken or invalid.\n");
        }
    }

    public User loginUser(String username, String password) {
        try {
            PreparedStatement checkUser = connection
                    .prepareStatement("SELECT * FROM Users WHERE Username = ? AND Password = ?");
            checkUser.setString(1, username);
            checkUser.setString(2, password);
            ResultSet result = checkUser.executeQuery();

            if (result.next()) {
                currentUser = new User(username, password);
                System.out.println("Login successful for user: " + username + "\n");
                return currentUser;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Login failed. Please check your credentials.\n");
        return null;
    }

    public void displayRoutes() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM Routes");

            System.out.println("Available Bus Routes:");
            int routeNumber = 1;
            while (resultSet.next()) {
                String source = resultSet.getString("Source");
                String destination = resultSet.getString("Destination");
                double adultFare = resultSet.getDouble("AdultFare");
                System.out.println(routeNumber + ". " + source + " to " + destination + " - Adult Fare: $" + adultFare);
                routeNumber++;
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void displayAvailableSeats(int routeIndex) {
        if (routeIndex < 0 || routeIndex >= routes.size()) {
            System.out.println("Invalid route selection.\n");
            return;
        }

        BusRoute selectedRoute = routes.get(routeIndex);
        Seat[][] seats = selectedRoute.getSeats();

        System.out.println(
                "Available Seats for " + selectedRoute.getSource() + " to " + selectedRoute.getDestination() + ":");

        int maxSeatNumber = 35; // Fixed number of seats

        for (int row = 0; row < seats.length; row++) {
            for (int col = 0; col < seats[row].length; col++) {
                Seat seat = seats[row][col];
                int seatNumber = seat.getSeatNumber();

                if (seat.isAvailable()) {
                    if (col == 2) {
                        System.out.print("  ");
                    }
                    System.out.printf("%-3d ", seatNumber);
                } else {
                    System.out.print("X   ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public void bookTicket(int routeIndex, int numTickets) {
        if (currentUser == null) {
            System.out.println("You must log in to book a ticket.\n");
            return;
        }

        if (routeIndex < 0 || routeIndex >= routes.size()) {
            System.out.println("Invalid route selection.\n");
            return;
        }

        BusRoute selectedRoute = routes.get(routeIndex);
        Seat[][] seats = selectedRoute.getSeats();
        int maxSeatNumber = 35; // Fixed number of seats

        if (numTickets < 1) {
            System.out.println("Invalid number of tickets. Please enter a valid number.\n");
            return;
        }

        if (numTickets > maxSeatNumber) {
            System.out.println("You can't book more than " + maxSeatNumber + " tickets for this route.\n");
            return;
        }

        for (int i = 0; i < numTickets; i++) {
            int seatNumber;
            while (true) {
                System.out.print("Enter the seat number for ticket " + (i + 1) + ": ");
                if (scanner.hasNextInt()) {
                    seatNumber = scanner.nextInt();
                    if (seatNumber < 1 || seatNumber > maxSeatNumber) {
                        System.out.println("Invalid seat number. Please choose a valid seat.\n");
                    } else {
                        int row = (seatNumber - 1) / 5;
                        int col = (seatNumber - 1) % 5;
                        Seat selectedSeat = seats[row][col];
                        if (!selectedSeat.isAvailable()) {
                            System.out.println("Seat " + seatNumber
                                    + " is already booked or invalid. Please choose an available seat.\n");
                        } else {
                            selectedSeat.book();
                            break;
                        }
                    }
                } else {
                    System.out.println("Invalid input. Please enter a valid numeric seat number.");
                    scanner.nextLine(); // Clear the input buffer
                }
            }

            String passengerName;
            scanner.nextLine();
            while (true) {
                System.out.print("Enter the passenger name for ticket " + (i + 1) + ": ");
                passengerName = scanner.nextLine();
                if (!passengerName.isEmpty()) {
                    break;
                } else {
                    System.out.println("Passenger name cannot be empty. Please enter a valid name.");
                }
            }

            double ticketFare = selectedRoute.getAdultFare(); // Calculate individual ticket fare

            try {
                PreparedStatement insertBooking = connection.prepareStatement(
                        "INSERT INTO Bookings (Username, Route, PassengerName, SeatNumber, TicketFare) VALUES (?, ?, ?, ?, ?)");
                insertBooking.setString(1, currentUser.getUsername());
                insertBooking.setString(2, selectedRoute.getSource() + " to " + selectedRoute.getDestination());
                insertBooking.setString(3, passengerName);
                insertBooking.setInt(4, seatNumber);
                insertBooking.setDouble(5, ticketFare); // Store individual ticket fare
                insertBooking.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        double totalFare = numTickets * selectedRoute.getAdultFare();
        System.out.println("Tickets booked successfully for " + numTickets + " seat(s) on " +
                selectedRoute.getSource() + " to " + selectedRoute.getDestination() +
                " route. Total Fare: $" + totalFare + "\n");
    }

    public void addRoute(String source, String destination, double adultFare) {
        BusRoute newRoute = new BusRoute(source, destination, adultFare);
        routes.add(newRoute);

        try {
            PreparedStatement insertRoute = connection.prepareStatement(
                    "INSERT INTO Routes (Source, Destination, AdultFare) VALUES (?, ?, ?)");
            insertRoute.setString(1, source);
            insertRoute.setString(2, destination);
            insertRoute.setDouble(3, adultFare);
            insertRoute.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BusTicketBookingSystem system = new BusTicketBookingSystem();
        system.addRoute("Chennai", "Bangalore", 100.0);
        system.addRoute("Mumbai", "Delhi", 150.0);
        system.addRoute("Kolkata", "Hyderabad", 120.0);
        system.addRoute("Jaipur", "Ahmedabad", 90.0);
        system.addRoute("Pune", "Goa", 80.0);
        system.addRoute("Lucknow", "Varanasi", 70.0);

        boolean running = true;

        while (running) {
            if (system.currentUser == null) {
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");
                int choice = system.scanner.nextInt();
                system.scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter username: ");
                        String username = system.scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = system.scanner.nextLine();
                        system.registerUser(username, password);
                        break;
                    case 2:
                        System.out.print("Enter username: ");
                        String loginUsername = system.scanner.nextLine();
                        System.out.print("Enter password: ");
                        String loginPassword = system.scanner.nextLine();
                        system.loginUser(loginUsername, loginPassword);
                        break;
                    case 3:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.\n");
                }
            } else {
                System.out.println("1. See Bus Routes");
                System.out.println("2. Logout");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");
                int choice = system.scanner.nextInt();
                system.scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        system.displayRoutes();
                        System.out.print("Enter the route number to book a ticket or 0 to go back: ");
                        int routeChoice = system.scanner.nextInt();
                        system.scanner.nextLine(); // Consume newline

                        if (routeChoice == 0) {
                            // Go back to the main menu
                            break;
                        } else if (routeChoice > 0 && routeChoice <= system.routes.size()) {
                            system.displayAvailableSeats(routeChoice - 1);
                            System.out.print("Enter the number of tickets you want to book (0 to go back): ");
                            int numTickets = system.scanner.nextInt();
                            system.scanner.nextLine(); // Consume newline
                            system.bookTicket(routeChoice - 1, numTickets);
                        } else {
                            System.out.println("Invalid route selection.\n");
                        }
                        break;
                    case 2:
                        system.currentUser = null;
                        System.out.println("Logged out successfully.\n");
                        break;
                    case 3:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.\n");
                }
            }
        }
        System.out.println("Thank you for using the Bus Ticket Booking System!");
        system.scanner.close();
    }
}
