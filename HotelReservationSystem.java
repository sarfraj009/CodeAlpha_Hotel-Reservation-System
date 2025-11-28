
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class HotelReservationSystem {

    public static void main(String[] args) {
        Hotel hotel = new Hotel("Simple Inn");
        hotel.loadReservations(); // load from reservations.dat if present
        hotel.initSampleRoomsIfEmpty(); // create sample rooms if none exist
        Scanner sc = new Scanner(System.in);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        System.out.println("=== Welcome to " + hotel.getName() + " Reservation System ===");
        boolean running = true;
        while (running) {
            System.out.println("\nChoose an option:");
            System.out.println("1) List all rooms");
            System.out.println("2) Search available rooms");
            System.out.println("3) Make a reservation");
            System.out.println("4) Cancel a reservation");
            System.out.println("5) View reservation details");
            System.out.println("6) List my reservations (by email)");
            System.out.println("7) Exit");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        hotel.printAllRooms();
                        break;
                    case "2": {
                        System.out.print("Enter category (Standard/Deluxe/Suite or ALL): ");
                        String cat = sc.nextLine().trim();
                        System.out.print("Check-in date (yyyy-MM-dd): ");
                        LocalDate in = LocalDate.parse(sc.nextLine().trim(), fmt);
                        System.out.print("Check-out date (yyyy-MM-dd): ");
                        LocalDate out = LocalDate.parse(sc.nextLine().trim(), fmt);
                        List<Room> avail = hotel.searchAvailableRooms(cat, in, out);
                        if (avail.isEmpty()) {
                            System.out.println("No rooms available for those dates/category.");
                        } else {
                            System.out.println("Available rooms:");
                            avail.forEach(System.out::println);
                        }
                        break;
                    }
                    case "3": {
                        System.out.print("Your name: ");
                        String name = sc.nextLine().trim();
                        System.out.print("Your email: ");
                        String email = sc.nextLine().trim();
                        System.out.print("Category (Standard/Deluxe/Suite): ");
                        String category = sc.nextLine().trim();
                        System.out.print("Check-in date (yyyy-MM-dd): ");
                        LocalDate in = LocalDate.parse(sc.nextLine().trim(), fmt);
                        System.out.print("Check-out date (yyyy-MM-dd): ");
                        LocalDate out = LocalDate.parse(sc.nextLine().trim(), fmt);
                        List<Room> avail = hotel.searchAvailableRooms(category, in, out);
                        if (avail.isEmpty()) {
                            System.out.println("No available rooms. Try different dates or category.");
                        } else {
                            System.out.println("Choose room id from available list:");
                            avail.forEach(System.out::println);
                            System.out.print("Enter room id: ");
                            int rid = Integer.parseInt(sc.nextLine().trim());
                            Room chosen = hotel.getRoomById(rid);
                            if (chosen == null) {
                                System.out.println("Invalid room id.");
                                break;
                            }
                            long nights = ChronoUnit.DAYS.between(in, out);
                            if (nights <= 0) {
                                System.out.println("Check-out must be after check-in.");
                                break;
                            }
                            double amount = chosen.getPricePerNight() * nights;
                            System.out.printf("Total amount for %d nights: %.2f\n", nights, amount);
                            System.out.print("Proceed to payment? (yes/no): ");
                            String payConfirm = sc.nextLine().trim().toLowerCase();
                            if (!payConfirm.equals("yes")) {
                                System.out.println("Reservation cancelled by user before payment.");
                                break;
                            }
                            boolean paid = PaymentProcessor.processPayment(amount);
                            if (!paid) {
                                System.out.println("Payment failed. Reservation not completed.");
                                break;
                            }
                            Reservation res = hotel.makeReservation(name, email, chosen.getId(), in, out);
                            if (res != null) {
                                System.out.println("Reservation successful! Details:");
                                System.out.println(res);
                                hotel.saveReservations();
                            } else {
                                System.out.println("Failed to create reservation (room may no longer be available).");
                            }
                        }
                        break;
                    }
                    case "4": {
                        System.out.print("Enter reservation ID to cancel: ");
                        String rid = sc.nextLine().trim();
                        boolean ok = hotel.cancelReservation(rid);
                        if (ok) {
                            hotel.saveReservations();
                            System.out.println("Reservation cancelled and saved.");
                        } else {
                            System.out.println("Reservation not found or could not be cancelled.");
                        }
                        break;
                    }
                    case "5": {
                        System.out.print("Enter reservation ID: ");
                        String rid = sc.nextLine().trim();
                        Reservation r = hotel.getReservationById(rid);
                        if (r != null) {
                            System.out.println(r);
                        } else {
                            System.out.println("Not found.");
                        }
                        break;
                    }
                    case "6": {
                        System.out.print("Enter your email: ");
                        String email = sc.nextLine().trim();
                        List<Reservation> list = hotel.getReservationsByEmail(email);
                        if (list.isEmpty()) {
                            System.out.println("No reservations found for this email.");
                        } else {
                            list.forEach(System.out::println);
                        }
                        break;
                    }
                    case "7":
                        running = false;
                        hotel.saveReservations();
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
        sc.close();
    }
}

class Hotel {

    private String name;
    private List<Room> rooms = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();
    private static final String RES_FILE = "reservations.dat";

    public Hotel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void initSampleRoomsIfEmpty() {
        if (!rooms.isEmpty()) {
            return;
        }
        rooms.add(new Room(1, "Standard", 1000.0));
        rooms.add(new Room(2, "Standard", 1500.0));
        rooms.add(new Room(3, "Deluxe", 2000.0));
        rooms.add(new Room(4, "Deluxe", 3000.0));
        rooms.add(new Room(5, "Suite", 5000.0));
    }

    public void printAllRooms() {
        if (rooms.isEmpty()) {
            System.out.println("No rooms defined.");
            return;
        }
        System.out.println("Rooms:");
        rooms.forEach(System.out::println);
    }

    public Room getRoomById(int id) {
        return rooms.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    public List<Room> searchAvailableRooms(String category, LocalDate checkIn, LocalDate checkOut) {
        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            return Collections.emptyList();
        }
        String cat = category == null ? "ALL" : category.trim();
        List<Room> candidate = new ArrayList<>();
        for (Room r : rooms) {
            if (!cat.equalsIgnoreCase("ALL") && !r.getCategory().equalsIgnoreCase(cat)) {
                continue;
            }
            boolean free = isRoomAvailable(r.getId(), checkIn, checkOut);
            if (free) {
                candidate.add(r);
            }
        }
        return candidate;
    }

    private boolean isRoomAvailable(int roomId, LocalDate in, LocalDate out) {
        for (Reservation res : reservations) {
            if (res.getRoomId() != roomId) {
                continue;
            }
            // reservations stored as [checkIn, checkOut) semantics
            LocalDate rIn = res.getCheckIn();
            LocalDate rOut = res.getCheckOut();
            // overlap if (in < rOut) && (rIn < out)
            if (in.isBefore(rOut) && rIn.isBefore(out)) {
                return false;
            }
        }
        return true;
    }

    public Reservation makeReservation(String guestName, String guestEmail, int roomId, LocalDate checkIn, LocalDate checkOut) {
        if (!isRoomAvailable(roomId, checkIn, checkOut)) {
            return null;
        }
        Room r = getRoomById(roomId);
        if (r == null) {
            return null;
        }
        Reservation res = new Reservation(UUID.randomUUID().toString(), guestName, guestEmail, roomId, checkIn, checkOut, r.getPricePerNight());
        reservations.add(res);
        return res;
    }

    public boolean cancelReservation(String reservationId) {
        Optional<Reservation> opt = reservations.stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst();
        if (!opt.isPresent()) {
            return false;
        }
        reservations.remove(opt.get());
        return true;
    }

    public Reservation getReservationById(String id) {
        return reservations.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    public List<Reservation> getReservationsByEmail(String email) {
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getGuestEmail().equalsIgnoreCase(email)) {
                out.add(r);
            }
        }
        return out;
    }

    public void saveReservations() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RES_FILE))) {
            oos.writeObject(reservations);
            // optionally store rooms separately if needed (here rooms are in-memory)
        } catch (IOException e) {
            System.err.println("Failed to save reservations: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadReservations() {
        File f = new File(RES_FILE);
        if (!f.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(RES_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                this.reservations = (List<Reservation>) obj;
            }
        } catch (Exception e) {
            System.err.println("Could not load reservations (starting fresh): " + e.getMessage());
            this.reservations = new ArrayList<>();
        }
    }
}

class Room implements Serializable {

    private int id;
    private String category;
    private double pricePerNight;

    public Room(int id, String category, double pricePerNight) {
        this.id = id;
        this.category = category;
        this.pricePerNight = pricePerNight;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    @Override
    public String toString() {
        return String.format("Room[id=%d, category=%s, price/night=%.2f]", id, category, pricePerNight);
    }
}

class Reservation implements Serializable {

    private String id;
    private String guestName;
    private String guestEmail;
    private int roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private double pricePerNight;

    public Reservation(String id, String guestName, String guestEmail, int roomId, LocalDate checkIn, LocalDate checkOut, double pricePerNight) {
        this.id = id;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.pricePerNight = pricePerNight;
    }

    public String getId() {
        return id;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public int getRoomId() {
        return roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public double getTotalAmount() {
        return getNights() * pricePerNight;
    }

    @Override
    public String toString() {
        return String.format("Reservation[id=%s, guest=%s, email=%s, room=%d, %s -> %s, nights=%d, total=%.2f]",
                id, guestName, guestEmail, roomId, checkIn, checkOut, getNights(), getTotalAmount());
    }
}

class PaymentProcessor {

    private static final Random rand = new Random();

    public static boolean processPayment(double amount) {
        double chance = rand.nextDouble();
        boolean success = chance < 0.90;
        System.out.printf("Processing payment of %.2f ... %s\n", amount, success ? "SUCCESS" : "FAILED");
        return success;
    }
}
