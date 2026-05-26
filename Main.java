import java.util.*;

enum RefundStatus {
    PENDING, UNDER_REVIEW, EVIDENCE_SUBMITTED, APPROVED, REJECTED, PARTIALLY_APPROVED, COMPLETED
}

class Order {
    private final int orderId;
    private final int customerId;
    private final String productName;
    private final double price;

    public Order(int orderId, int customerId, String productName, double price) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productName = productName;
        this.price = price;
    }

    public int getOrderId() { return orderId; }
    public int getCustomerId() { return customerId; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
}

class UserProfile {
    private final int userId;
    private final String name;
    private final String email;
    private final String role;

    public UserProfile(int userId, String name, String email, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}

class RefundRequest {
    private final int refundId;
    private final int orderId;
    private final double requestedAmount;
    private RefundStatus status;

    public RefundRequest(int refundId, int orderId, double amount) {
        this.refundId = refundId;
        this.orderId = orderId;
        this.requestedAmount = amount;
        this.status = RefundStatus.PENDING;
    }

    public int getRefundId() { return refundId; }
    public int getOrderId() { return orderId; }
    public double getRequestedAmount() { return requestedAmount; }
    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }
}

class RefundException extends Exception {
    public RefundException(String message) { super(message); }
}

class Database {
    private final Map<Integer, Order> orders = new HashMap<>();
    private final Map<Integer, UserProfile> users = new HashMap<>();
    private final Map<Integer, RefundRequest> refundRequests = new HashMap<>();
    private final Map<Integer, List<Order>> orderHistory = new HashMap<>();
    private int nextRefundId = 100;

    public Database() {
        users.put(10, new UserProfile(10, "Μαρία Γεωργίου", "maria@email.com", "Customer"));
        users.put(20, new UserProfile(20, "Γιώργος Παπαδόπουλος", "george@email.com", "Producer"));

        orders.put(5005, new Order(5005, 10, "Βιολογικές Ντομάτες", 45.0));
        orderHistory.put(10, Arrays.asList(
                new Order(5001, 10, "Φρέσκο Γάλα", 3.5),
                new Order(5005, 10, "Βιολογικές Ντομάτες", 45.0)
        ));
    }

    public Order findOrderById(int orderId) throws RefundException {
        Order order = orders.get(orderId);
        if (order == null) throw new RefundException("Παραγγελία #" + orderId + " δεν βρέθηκε.");
        return order;
    }

    public List<Order> getOrderHistory(int customerId) {
        return orderHistory.getOrDefault(customerId, Collections.emptyList());
    }

    public UserProfile getUserProfile(int userId) throws RefundException {
        UserProfile p = users.get(userId);
        if (p == null) throw new RefundException("Προφίλ χρήστη #" + userId + " δεν βρέθηκε.");
        return p;
    }

    public RefundRequest saveRefundRequest(RefundRequest request) {
        refundRequests.put(request.getRefundId(), request);
        return request;
    }

    public RefundRequest findRefundRequestById(int refundId) throws RefundException {
        RefundRequest req = refundRequests.get(refundId);
        if (req == null) throw new RefundException("Αίτημα επιστροφής #" + refundId + " δεν βρέθηκε.");
        return req;
    }

    public void updateRefundStatus(int refundId, RefundStatus newStatus) throws RefundException {
        RefundRequest req = findRefundRequestById(refundId);
        req.setStatus(newStatus);
    }

    public void deleteRefundRequest(int refundId) throws RefundException {
        if (refundRequests.remove(refundId) == null)
            throw new RefundException("Δεν ήταν δυνατή η διαγραφή του αιτήματος #" + refundId);
    }

    public int generateNextRefundId() { return ++nextRefundId; }

    public boolean processPayment(int fromProducerId, int toCustomerId, double amount) throws RefundException {
        System.out.printf("[DB] Αυτόματη χρέωση %.2f€ από παραγωγό #%d, πίστωση σε πελάτη #%d.\n",
                amount, fromProducerId, toCustomerId);
        return true;
    }
}

interface RefundSubmissionScreen {
    void showForm();
    void showSubmissionSuccess(int refundId);
}

class ConsoleRefundSubmissionScreen implements RefundSubmissionScreen {
    public void showForm() { System.out.println("[Οθόνη Αποστολής Refund] Φόρτωση φόρμας επιστροφής."); }
    public void showSubmissionSuccess(int refundId) {
        System.out.println("[Οθόνη Αποστολής Refund] Το αίτημα #" + refundId + " υποβλήθηκε επιτυχώς.");
    }
}

interface RequestUpdateScreen {
    void notifyNewRequest(int refundId);
}

class ConsoleRequestUpdateScreen implements RequestUpdateScreen {
    public void notifyNewRequest(int refundId) {
        System.out.println("[Οθόνη Ενημέρωσης Αιτήματος] Νέο αίτημα #" + refundId + " εμφανίστηκε.");
    }
}

interface RequestDisplayScreen {
    void showRequestDetails(RefundRequest request, Order order, UserProfile customer);
}

class ConsoleRequestDisplayScreen implements RequestDisplayScreen {
    public void showRequestDetails(RefundRequest request, Order order, UserProfile customer) {
        System.out.println("[Οθόνη Εμφάνισης Αιτήματος] Στοιχεία αιτήματος #" + request.getRefundId());
        System.out.println("   Παραγγελία: " + order.getProductName() + " (" + order.getPrice() + "€)");
        System.out.println("   Πελάτης: " + customer.getName());
    }
}

interface AnalysisScreen {
    void displayHistory(List<Order> history, UserProfile customer, UserProfile producer);
}

class ConsoleAnalysisScreen implements AnalysisScreen {
    public void displayHistory(List<Order> history, UserProfile customer, UserProfile producer) {
        System.out.println("[Οθόνη Ανάλυσης Αιτήματος] Ιστορικό παραγγελιών πελάτη:");
        history.forEach(o -> System.out.println("   - " + o.getProductName() + " (" + o.getPrice() + "€)"));
        System.out.println("   Προφίλ πελάτη: " + customer.getName() + " (" + customer.getEmail() + ")");
        System.out.println("   Προφίλ παραγωγού: " + producer.getName() + " (" + producer.getEmail() + ")");
    }
}

interface EvidenceScreen {
    void requestEvidence(int refundId);
    void showUploadConfirmation(String evidence);
}

class ConsoleEvidenceScreen implements EvidenceScreen {
    public void requestEvidence(int refundId) {
        System.out.println("[Οθόνη Ζήτησης Αποδεικτικών] Ζητήθηκαν αποδεικτικά για το αίτημα #" + refundId);
    }
    public void showUploadConfirmation(String evidence) {
        System.out.println("[Οθόνη Εμφάνισης Αποδεικτικών] Το αρχείο '" + evidence + "' ανέβηκε επιτυχώς.");
    }
}

interface CommunicationScreen {
    void openChat(int producerId);
    String getProducerDecision();
}

class ConsoleCommunicationScreen implements CommunicationScreen {
    private String simulatedDecision = "approved";

    public void setSimulatedDecision(String decision) { this.simulatedDecision = decision; }

    public void openChat(int producerId) {
        System.out.println("[Οθόνη Επικοινωνίας] Συνομιλία με παραγωγό #" + producerId + "...");
    }

    public String getProducerDecision() {
        System.out.println("[Οθόνη Επικοινωνίας] Απόφαση παραγωγού: " + simulatedDecision);
        return simulatedDecision;
    }
}

interface CustomerNotificationScreen {
    void showFullRefund(double amount);
    void showPartialRefund(double amount);
    void showRejection(int refundId);
}

class ConsoleCustomerNotificationScreen implements CustomerNotificationScreen {
    public void showFullRefund(double amount) {
        System.out.println("[Οθόνη Ενημέρωσης Πελάτη] Πλήρης επιστροφή " + amount + "€ έχει πραγματοποιηθεί.");
    }
    public void showPartialRefund(double amount) {
        System.out.println("[Οθόνη Μερικής Επιστροφής] Μερική επιστροφή " + amount + "€ έχει εγκριθεί και θα πιστωθεί.");
    }
    public void showRejection(int refundId) {
        System.out.println("[Οθόνη Μη Έγκρισης Αιτήματος] Το αίτημα #" + refundId + " απορρίφθηκε.");
    }
}

class RefundController {
    private final Database db;
    private final RefundSubmissionScreen submissionScreen;
    private final RequestUpdateScreen requestUpdateScreen;
    private final RequestDisplayScreen requestDisplayScreen;
    private final AnalysisScreen analysisScreen;
    private final EvidenceScreen evidenceScreen;
    private final CommunicationScreen communicationScreen;
    private final CustomerNotificationScreen notificationScreen;

    public RefundController(Database db,
                            RefundSubmissionScreen submissionScreen,
                            RequestUpdateScreen requestUpdateScreen,
                            RequestDisplayScreen requestDisplayScreen,
                            AnalysisScreen analysisScreen,
                            EvidenceScreen evidenceScreen,
                            CommunicationScreen communicationScreen,
                            CustomerNotificationScreen notificationScreen) {
        this.db = db;
        this.submissionScreen = submissionScreen;
        this.requestUpdateScreen = requestUpdateScreen;
        this.requestDisplayScreen = requestDisplayScreen;
        this.analysisScreen = analysisScreen;
        this.evidenceScreen = evidenceScreen;
        this.communicationScreen = communicationScreen;
        this.notificationScreen = notificationScreen;
    }

    public int submitRefundRequest(int orderId, double amount) throws RefundException {
        Order order = db.findOrderById(orderId);
        submissionScreen.showForm();

        int refundId = db.generateNextRefundId();
        RefundRequest request = new RefundRequest(refundId, order.getOrderId(), amount);
        db.saveRefundRequest(request);

        requestUpdateScreen.notifyNewRequest(refundId);
        submissionScreen.showSubmissionSuccess(refundId);
        return refundId;
    }

    public void adminViewRequest(int refundId) throws RefundException {
        RefundRequest request = db.findRefundRequestById(refundId);
        Order order = db.findOrderById(request.getOrderId());
        UserProfile customer = db.getUserProfile(order.getCustomerId());
        requestDisplayScreen.showRequestDetails(request, order, customer);
    }

    public void adminAnalyzeRequest(int refundId) throws RefundException {
        RefundRequest request = db.findRefundRequestById(refundId);
        Order order = db.findOrderById(request.getOrderId());

        UserProfile customer = db.getUserProfile(order.getCustomerId());
        UserProfile producer = db.getUserProfile(20);

        List<Order> history = db.getOrderHistory(customer.getUserId());
        analysisScreen.displayHistory(history, customer, producer);

        db.updateRefundStatus(refundId, RefundStatus.UNDER_REVIEW);
    }

    public void requestEvidence(int refundId, String evidenceFile) throws RefundException {
        evidenceScreen.requestEvidence(refundId);
        evidenceScreen.showUploadConfirmation(evidenceFile);
        db.updateRefundStatus(refundId, RefundStatus.EVIDENCE_SUBMITTED);
    }

    public void processProducerDecision(int refundId) throws RefundException {
        RefundRequest request = db.findRefundRequestById(refundId);
        Order order = db.findOrderById(request.getOrderId());
        UserProfile customer = db.getUserProfile(order.getCustomerId());

        UserProfile producer = db.getUserProfile(20);

        communicationScreen.openChat(producer.getUserId());
        String decision = communicationScreen.getProducerDecision();

        if ("approved".equalsIgnoreCase(decision)) {
            db.updateRefundStatus(refundId, RefundStatus.APPROVED);
            db.processPayment(producer.getUserId(), customer.getUserId(), request.getRequestedAmount());
            db.updateRefundStatus(refundId, RefundStatus.COMPLETED);
            notificationScreen.showFullRefund(request.getRequestedAmount());
        } else if ("rejected".equalsIgnoreCase(decision)) {
            db.updateRefundStatus(refundId, RefundStatus.REJECTED);
            db.deleteRefundRequest(refundId);
            notificationScreen.showRejection(refundId);
        } else if (decision.startsWith("partial:")) {
            double partialAmount = Double.parseDouble(decision.split(":")[1]);
            if (partialAmount > request.getRequestedAmount() || partialAmount <= 0)
                throw new RefundException("Μη έγκυρο ποσό μερικής επιστροφής.");
            db.updateRefundStatus(refundId, RefundStatus.PARTIALLY_APPROVED);
            db.processPayment(producer.getUserId(), customer.getUserId(), partialAmount);
            notificationScreen.showPartialRefund(partialAmount);
            db.updateRefundStatus(refundId, RefundStatus.COMPLETED);
        } else {
            throw new RefundException("Άγνωστη απόφαση παραγωγού: " + decision);
        }
    }
}

public class Main {
    public static void main(String[] args) throws RefundException {
        Database database = new Database();
        ConsoleRefundSubmissionScreen submissionScreen = new ConsoleRefundSubmissionScreen();
        ConsoleRequestUpdateScreen requestUpdateScreen = new ConsoleRequestUpdateScreen();
        ConsoleRequestDisplayScreen requestDisplayScreen = new ConsoleRequestDisplayScreen();
        ConsoleAnalysisScreen analysisScreen = new ConsoleAnalysisScreen();
        ConsoleEvidenceScreen evidenceScreen = new ConsoleEvidenceScreen();
        ConsoleCommunicationScreen communicationScreen = new ConsoleCommunicationScreen();
        ConsoleCustomerNotificationScreen notificationScreen = new ConsoleCustomerNotificationScreen();

        RefundController controller = new RefundController(
                database, submissionScreen, requestUpdateScreen, requestDisplayScreen,
                analysisScreen, evidenceScreen, communicationScreen, notificationScreen
        );

        System.out.println("===== USE CASE 10: ΠΛΗΡΗΣ ΡΟΗ (ΕΓΚΡΙΣΗ) =====");
        int refundId = controller.submitRefundRequest(5005, 45.0);

        controller.adminViewRequest(refundId);

        controller.adminAnalyzeRequest(refundId);

        controller.requestEvidence(refundId, "κατεστραμμένη_συσκευασία.jpg");

        communicationScreen.setSimulatedDecision("approved");
        controller.processProducerDecision(refundId);

        System.out.println("\n===== ΕΝΑΛΛΑΚΤΙΚΗ ΡΟΗ 2: ΜΕΡΙΚΗ ΕΠΙΣΤΡΟΦΗ =====");
        int refundId2 = controller.submitRefundRequest(5005, 45.0);
        controller.adminViewRequest(refundId2);
        controller.adminAnalyzeRequest(refundId2);
        controller.requestEvidence(refundId2, "αποδειξη.jpg");
        communicationScreen.setSimulatedDecision("partial:20.00");
        controller.processProducerDecision(refundId2);

        System.out.println("\n===== ΕΝΑΛΛΑΚΤΙΚΗ ΡΟΗ 1: ΑΠΟΡΡΙΨΗ =====");
        int refundId3 = controller.submitRefundRequest(5005, 45.0);
        controller.adminViewRequest(refundId3);
        controller.adminAnalyzeRequest(refundId3);
        controller.requestEvidence(refundId3, "φωτο.jpg");
        communicationScreen.setSimulatedDecision("rejected");
        controller.processProducerDecision(refundId3);

        System.out.println("\nΗ εκτέλεση ολοκληρώθηκε με επιτυχία. Όλες οι ροές καλύφθηκαν.");
    }
}
