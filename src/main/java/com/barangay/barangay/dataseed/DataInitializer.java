package com.barangay.barangay.dataseed;

import com.barangay.barangay.audit.model.AuditLog;
import com.barangay.barangay.audit.repository.AuditLogRepository;
import com.barangay.barangay.blotter.model.EvidenceType;
import com.barangay.barangay.blotter.repository.*;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.employee.model.Employee;
import com.barangay.barangay.enumerated.*;
import com.barangay.barangay.lupon.repository.PangkatCompositionRepository;
import com.barangay.barangay.permission.model.Permission;
import com.barangay.barangay.person.model.*;
import com.barangay.barangay.employee.repository.EmployeeRepository;
import com.barangay.barangay.person.repository.PersonRepository;
import com.barangay.barangay.person.repository.ResidentRepository;
import com.barangay.barangay.role.model.Role;
import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.department.repository.DepartmentRepository;
import com.barangay.barangay.permission.repository.PermissionRepository;
import com.barangay.barangay.role.repository.RoleRepository;
import com.barangay.barangay.admin_management.repository.Root_AdminRepository;
import com.barangay.barangay.vawc.model.ViolenceType;
import com.barangay.barangay.vawc.repository.VawcCaseRepository;
import com.barangay.barangay.vawc.repository.ViolenceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final Root_AdminRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    //blotter
    private final EvidenceTypeRepository evidenceTypeRepository;

    private final PangkatCompositionRepository pangkatCompositionRepository;

    private final PersonRepository personRepository;
    private final ResidentRepository residentRepository;
    private final EmployeeRepository employeeRepository;
    private final ViolenceTypeRepository violenceTypeRepository;
    private final VawcCaseRepository vawcCaseRepository;


    @Override
    @Transactional
    public void run(String... args) {


        Department adminDept = createDeptIfNotFound("ADMINISTRATION");
        createDeptIfNotFound("VAWC");
        createDeptIfNotFound("BLOTTER");
        createDeptIfNotFound("KAPITANA");
        createDeptIfNotFound("BCPC");
        createDeptIfNotFound("CLEARANCE");
        createDeptIfNotFound("LUPONG_TAGAPAMAYAPA");
        createDeptIfNotFound("FTJS");
        createDeptIfNotFound("ROOT_ADMIN");


        if (residentRepository.count() == 0) {
            seedResidents(100);
        }










        // ── Permissions ──────────────────────────────────────────────────────

        // BLOTTER MODULE PERMISSIONS
         createPermIfNotFound("View Records ");
        createPermIfNotFound("Manage Mediation");
        createPermIfNotFound("Manage Lupon Escalation");

        //global
        createPermIfNotFound("View Cases ");
        createPermIfNotFound("Create Case Entry");
        createPermIfNotFound("Archive Cases");
        createPermIfNotFound("Resolve & Finalize Case");
        createPermIfNotFound("Manage Case notes");
        createPermIfNotFound("Manage Reports");
        createPermIfNotFound("Issue Referral");
        createPermIfNotFound("Update Case information ");

        //lupon
        createPermIfNotFound("Manage Conciliation");

        //vawc
        createPermIfNotFound("Issue BPO");
        createPermIfNotFound("Manage Intervention");


        //clearance
        createPermIfNotFound("Issue Clearance");
        createPermIfNotFound("Edit Template");
        createPermIfNotFound("View Revenue Reports");

        //ftjs
        createPermIfNotFound("Register new Applicant");
        createPermIfNotFound("Issue ftjs Certificate");
        createPermIfNotFound("View ftjs Records");
        createPermIfNotFound("Update Applicant Info");


        createPermIfNotFound("Create users");
        createPermIfNotFound("Edit users");
        createPermIfNotFound("View users");
        createPermIfNotFound("Lock users");
        createPermIfNotFound("Archive users");

        createPermIfNotFound("Create resident");
        createPermIfNotFound("View residents");
        createPermIfNotFound("Edit resident");
        createPermIfNotFound("Archive resident");

        createPermIfNotFound("Create Officer");
        createPermIfNotFound("View Officers");
        createPermIfNotFound("Edit Officer");
        createPermIfNotFound("Restore Archived");
        createPermIfNotFound("Case Re-open");

            createPermIfNotFound("View blotter cases");
            createPermIfNotFound("View blotter reports");
            createPermIfNotFound("View lupon cases");
            createPermIfNotFound("View lupon reports");
            createPermIfNotFound("View vawc reports");
            createPermIfNotFound("View vawc cases");
            createPermIfNotFound("View bcpc reports");
            createPermIfNotFound("View bcpc cases");
            createPermIfNotFound("View clerance issued");
            createPermIfNotFound("View clerance revenue");
            createPermIfNotFound("View ftjs issued");
            createPermIfNotFound("View ftjs reports");



        createEvidenceTypeIfNotFound("Medical Certificate");
        createEvidenceTypeIfNotFound("Medico-Legal Report");
        createEvidenceTypeIfNotFound("Psychological Evaluation Report");
        createEvidenceTypeIfNotFound("Dental Record");
        createEvidenceTypeIfNotFound("Lab Result (Toxicology/X-Ray/etc.)");

        // 2. Testimonial
        createEvidenceTypeIfNotFound("Affidavit / Sworn Statement");
        createEvidenceTypeIfNotFound("Complaint Affidavit");
        createEvidenceTypeIfNotFound("Witness Statement");
        createEvidenceTypeIfNotFound("Kagawad/Lupon Mediation Agreement");
        createEvidenceTypeIfNotFound("Police Blotter/Report Referral");
        createEvidenceTypeIfNotFound("Court Order / Subpoena");

        // 3. Digital
        createEvidenceTypeIfNotFound("Screenshot (Messenger/Viber/WhatsApp)");
        createEvidenceTypeIfNotFound("Screenshot (Facebook/Post/Comment)");
        createEvidenceTypeIfNotFound("Voice/Audio Recording");
        createEvidenceTypeIfNotFound("Email Print-out");
        createEvidenceTypeIfNotFound("SMS / Text Message Transcript");

        // 4. Visual
        createEvidenceTypeIfNotFound("Photograph (Incident Scene)");
        createEvidenceTypeIfNotFound("Photograph (Physical Injury)");
        createEvidenceTypeIfNotFound("Photograph (Property Damage)");
        createEvidenceTypeIfNotFound("CCTV Footage (Digital/USB)");
        createEvidenceTypeIfNotFound("Dashcam Footage");
        createEvidenceTypeIfNotFound("Hand-drawn Map or Sketch");

        // 5. Material
        createEvidenceTypeIfNotFound("Physical Item (Weapon/Tool/etc.)");
        createEvidenceTypeIfNotFound("Clothing or Personal Belonging");
        createEvidenceTypeIfNotFound("Illegal Substance (Turned over to Police)");
        createEvidenceTypeIfNotFound("Actual Damaged Equipment/Part");

        // 6. Financial
        createEvidenceTypeIfNotFound("Official Receipt / Proof of Payment");
        createEvidenceTypeIfNotFound("Contract / Lease Agreement");
        createEvidenceTypeIfNotFound("Land Title / Deed of Sale");

        createEvidenceTypeIfNotFound("Other Supporting Documents/Items");



        // ── VAWC Violence Types (RA 9262) ────────────────────────────────────
        // ── VAWC (RA 9262) Specifics ──────────────────────────────────────────
        createViolenceTypeIfNotFound(" PHYSICAL VIOLENCE", "Bodily or physical harm such as battery, assault, or physical maltreatment.");
        createViolenceTypeIfNotFound(" SEXUAL VIOLENCE", "Acts sexual in nature, including rape, sexual harassment, or forced sexual acts.");
        createViolenceTypeIfNotFound(" PSYCHOLOGICAL VIOLENCE", "Mental or emotional suffering, intimidation, stalking, or public ridicule.");
        createViolenceTypeIfNotFound(" ECONOMIC ABUSE", "Financial dependency, withdrawal of support, or controlling the victim's own money.");

// ── BCPC / CHILD ABUSE (RA 7610) Specifics ────────────────────────────
        createViolenceTypeIfNotFound(" CHILD ABUSE", "Physical, psychological, or sexual maltreatment of a minor (under 18).");
        createViolenceTypeIfNotFound(" NEGLECT", "Failure to provide basic needs like food, education, medical care, or abandonment.");
        createViolenceTypeIfNotFound(" CHILD LABOR", "Exploitation of children in harmful work or long hours that interfere with education.");
        createViolenceTypeIfNotFound("TRAFFICKING", "Recruitment or transport of children for exploitation or illegal acts.");
        createViolenceTypeIfNotFound(" CYBER-VIOLENCE / OSAEC", "Online sexual abuse of children, cyber-grooming, or sharing of sensitive materials.");

// ── OTHERS ────────────────────────────────────────────────────────────
        createViolenceTypeIfNotFound("OTHERS", "Other forms of violence or abuse not specifically categorized above.");


        // ── Relationship Types (Reference Data) ──────────────────────────────




        // ── Roles ────────────────────────────────────────────────────────────
        Role rootRole = roleRepository.findByRoleName("ROOT_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName("ROOT_ADMIN");
                    return roleRepository.save(role);
                });

        createRoleIfNotFound("ADMIN");
        createRoleIfNotFound("Desk Officer");
        createRoleIfNotFound("Focal Person");
        createRoleIfNotFound("Chair person");
        createRoleIfNotFound("Vice Chairperson");
        createRoleIfNotFound("Secretary");
        createRoleIfNotFound("Members");
        createRoleIfNotFound("Chairman");
        createRoleIfNotFound("Officials");
        createRoleIfNotFound("Captain");


        String rawPassword = "82219800Jeremiah!";
        String hashedContext = passwordEncoder.encode(rawPassword);

        User rootUser = userRepository.findByUsername("rootadmin")
                .orElseGet(() -> {

                    Person person = new Person();
                    person.setFirstName("Juan");
                    person.setLastName("Dela Cruz");
                    person = personRepository.save(person);


                    User root = new User();
                    root.setUsername("rootadmin");
                    root.setPassword(hashedContext);
                    root.setSystemEmail("nermamadronio@gmail.com");
                    root.setPerson(person);
                    root.setStatus(Status.ACTIVE);
                    root.setRole(rootRole);
                    root.setAllowedDepartments(new HashSet<>(Set.of(adminDept)));
                    root.setFailedAttempts(0);
                    root.setIsLocked(false);
                    return userRepository.save(root);
                });




        if (auditLogRepository.count() == 0) {
            System.out.println("Starting Audit Log Seeding for Dashboard Testing...");

            seedLogs(rootUser, Departments.VAWC,      "VAWC",      245);
            seedLogs(rootUser, Departments.BLOTTER,   "BLOTTER",   189);
            seedLogs(rootUser, Departments.BCPC,      "BCPC",      156);
            seedLogs(rootUser, Departments.FTJS,      "FTJS",       98);
            seedLogs(rootUser, Departments.CLEARANCE, "CLEARANCE", 312);

            List<AuditLog> criticalLogs = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                criticalLogs.add(AuditLog.builder()
                        .user(rootUser)
                        .department(Departments.ROOT_ADMIN)
                        .severity(Severity.CRITICAL)
                        .module("SECURITY")
                        .actionTaken("UNAUTHORIZED_ACCESS_ATTEMPT")
                        .reason("Detected multiple failed attempts from unrecognized IP")
                        .ipAddress("192.168.1.50")
                        .build());
            }
            auditLogRepository.saveAll(criticalLogs);

            seedHistoricalLogs(rootUser, 50);

            System.out.println("Dashboard testing data seeded successfully.");
        }


    }








    /**
     * FIX: Was hardcoding Departments.ROOT_ADMIN — now uses the dept param correctly.
     */
    private void seedLogs(User actor, Departments dept, String module, int count) {
        List<AuditLog> logsToSave = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logsToSave.add(AuditLog.builder()
                    .user(actor)
                    .department(dept)
                    .module(module)
                    .severity(Severity.INFO)
                    .actionTaken("CREATE_RECORD")
                    .reason("Initial migration data for " + module)
                    .ipAddress("127.0.0.1")
                    .build());
        }
        auditLogRepository.saveAll(logsToSave);
    }

    /**
     * Seeds historical logs with a backdated created_at timestamp.
     */
    private void seedHistoricalLogs(User actor, int count) {
        Departments dept = Departments.ROOT_ADMIN;
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1).minusDays(5);

        String roleName = (actor.getRole() != null)
                ? actor.getRole().getRoleName()
                : "SYSTEM";

        List<AuditLog> logsToSave = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logsToSave.add(AuditLog.builder()
                    .user(actor)
                    .department(dept)
                    .severity(Severity.INFO)
                    .module(roleName)
                    .actionTaken("HISTORICAL_LOG")
                    .reason("Generated log for " + roleName)
                    .ipAddress("127.0.0.1")
                    .build());
        }

        // Save all first, then batch-update created_at to backdate them
        List<AuditLog> savedLogs = auditLogRepository.saveAll(logsToSave);

        List<Object[]> batchArgs = savedLogs.stream()
                .map(log -> new Object[]{lastMonth, log.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(
                "UPDATE audit_logs SET created_at = ? WHERE id = ?",
                batchArgs
        );
    }

    private Department createDeptIfNotFound(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName(name);
                    return departmentRepository.save(dept);
                });
    }

    private Permission createPermIfNotFound(String name) {
        return permissionRepository.findByPermissionName(name)
                .orElseGet(() -> {
                    Permission perm = new Permission();
                    perm.setPermissionName(name);
                    return permissionRepository.save(perm);
                });
    }

    private void createRoleIfNotFound(String name) {
        if (roleRepository.findByRoleName(name).isEmpty()) {
            Role role = new Role();
            role.setRoleName(name);
            roleRepository.save(role);
        }
    }


    private void createViolenceTypeIfNotFound(String name, String description) {
        if (violenceTypeRepository.findByName(name).isEmpty()) {
            ViolenceType type = new ViolenceType();
            type.setName(name);
            type.setDescription(description);
            violenceTypeRepository.save(type);
            System.out.println("[VIOLENCE TYPE SEEDED] " + name);
        }
    }







    private void createEvidenceTypeIfNotFound(String name) {
        if (evidenceTypeRepository.findByTypeName(name).isEmpty()) {
            EvidenceType type = new EvidenceType();
            type.setTypeName(name);
            evidenceTypeRepository.save(type);
        }
    }




    private void seedResidents(int count) {
        String[] firstNames = {
                // Classic Male
                "Juan", "Jose", "Ricardo", "Roberto", "Fernando", "Francisco", "Antonio", "Jaime",
                "Benjamin", "Rodolfo", "Eduardo", "Manuel", "Ramon", "Ernesto", "Alfredo", "Domingo",
                "Renato", "Rodrigo", "Danilo", "Virgilio", "Armando", "Leonidas", "Crisanto", "Marcelo",
                "Conrado", "Wilfredo", "Rolando", "Edmundo", "Teodoro", "Gervacio", "Nestor", "Efren",
                "Gregorio", "Isidro", "Lorenzo", "Maximo", "Narciso", "Onofre", "Pascual", "Quintin",
                "Silverio", "Tomas", "Urbano", "Vicente", "Wenceslao", "Bernardo", "Cesar", "Diego",
                "Enrique", "Felipe", "Gilberto", "Hernan", "Ignacio", "Julio", "Mariano", "Oscar",
                "Pablo", "Rafael", "Amado", "Brigido", "Candido", "Diosdado", "Eusebio", "Faustino",
                "Gavino", "Hilarion", "Iluminado", "Juanito", "Ladislao", "Melanio", "Nicanor",
                "Olimpio", "Primitivo", "Restituto", "Saturnino", "Tiburcio", "Uldarico", "Vitaliano",
                "Zosimo", "Avelino", "Baldomero", "Casimiro", "Dalmacio", "Estanislao", "Fulgencio",
                "Gaudencio", "Hilario", "Ireneo", "Jacinto", "Lamberto", "Macedonio", "Nemesio",
                // Modern Male
                "Elmer", "Noel", "Rodel", "Arnel", "Jayson", "Mark", "Kevin", "Christian", "John",
                "Michael", "Carlo", "Angelo", "Patrick", "Jomar", "Dario", "Karl", "Bryan", "Ryan",
                "Nathan", "Aaron", "Adrian", "Alex", "Allen", "Arnold", "Arvin", "Benjo", "Bonn",
                "Cardo", "Chito", "Dante", "Dennis", "Dodong", "Edwin", "Eric", "Felix", "Gene",
                "Gerald", "Glen", "Harold", "Ivan", "Jeric", "Jerome", "Jobert", "Joel", "Joey",
                "Jonathan", "Jonjon", "Joshua", "Kenneth", "Lando", "Leo", "Leonard", "Lester",
                "Lloyd", "Louie", "Luke", "Marlon", "Martin", "Melvin", "Miguel", "Mike", "Myke",
                "Nelson", "Nino", "Norman", "Oliver", "Orly", "Peter", "Philip", "Randy", "Ray",
                "Rene", "Rex", "Rey", "Richard", "Ronnie", "Roy", "Rudy", "Sam", "Sammy", "Sean",
                "Sherwin", "Sonny", "Steve", "Tirso", "Tony", "Vic", "Wendell", "Willy", "Wilson",
                // Classic Female
                "Maria", "Elena", "Gloria", "Teresa", "Imelda", "Lourdes", "Corazon", "Remedios",
                "Carmelita", "Dolores", "Rosario", "Ligaya", "Perla", "Nenita", "Erlinda", "Felicitas",
                "Herminia", "Irene", "Josefina", "Lorena", "Natividad", "Ofelia", "Salome", "Teresita",
                "Ursula", "Yolanda", "Zenaida", "Esperanza", "Fe", "Gertrudes", "Isabelita",
                "Leonora", "Noemi", "Ophelia", "Rosalinda", "Thelma", "Vilma", "Adelaida", "Belen",
                "Cecilia", "Elisa", "Florencia", "Gina", "Hilda", "Ines", "Juanita", "Luz",
                "Mercedes", "Nora", "Olivia", "Pilar", "Rita", "Soledad", "Trinidad", "Victoria",
                "Amelia", "Basilisa", "Catalina", "Dominga", "Eufracia", "Felipa", "Generosa",
                "Hipolita", "Incarnacion", "Juana", "Lorenza", "Marcelina", "Narcisa", "Olimpia",
                "Perpetua", "Rafaela", "Segundina", "Timotea", "Valentina", "Wilhelmina", "Adora",
                "Benita", "Conching", "Divina", "Estrella", "Fely", "Ging", "Heidi", "Imee",
                // Modern Female
                "Karen", "Kristine", "Patricia", "Marites", "Rowena", "Shirley", "Analiza",
                "Bernadette", "Cristina", "Hazel", "Jennifer", "Maribel", "Daisy", "Gina", "Katrina",
                "Alma", "Andrea", "Angel", "Anna", "April", "Araceli", "Arlyn", "Baby", "Bambi",
                "Camille", "Carla", "Carmela", "Carol", "Charity", "Che", "Chelsea", "Cherry",
                "Cynthia", "Donna", "Dorothy", "Edna", "Elaine", "Ellen", "Emily", "Emma",
                "Evelyn", "Faith", "Fatima", "Fiona", "Flor", "Gigi", "Grace", "Hannah",
                "Jackie", "Jade", "Jamie", "Jana", "Jasmine", "Jean", "Jessica", "Joanna",
                "Joyce", "Julie", "Karina", "Kate", "Kathryn", "Lara", "Laura", "Leah",
                "Linda", "Lisa", "Liza", "Lovely", "Lyn", "Lynda", "Mabel", "Mae", "Mara",
                "Margaret", "Mary", "Megan", "Michelle", "Mila", "Mylene", "Nadia", "Nancy",
                "Nicole", "Nina", "Pamela", "Pearl", "Princess", "Rachel", "Rebecca", "Regina",
                "Rhea", "Rica", "Rina", "Rose", "Roxanne", "Ruby", "Ruth", "Sandra", "Sarah",
                "Sharon", "Sofia", "Stella", "Susan", "Tina", "Vanessa", "Veronica", "Vicky",
                "Vivian", "Wendy", "Xenia", "Yvonne", "Zara"
        };

        String[] lastNames = {
                // Common Filipino
                "Dela Cruz", "Garcia", "Reyes", "Ramos", "Mendoza", "Santos", "Flores", "Gonzales",
                "Bautista", "Villanueva", "Fernandez", "Cruz", "Lopez", "Castillo", "Gomez", "Pineda",
                "Madronio", "De Leon", "Mercado", "Rivera", "Torres", "Aquino", "Diaz", "Marquez",
                "Soriano", "Santiago", "Espiritu", "Navarro", "Aguilar", "Pascual", "Morales", "Perez",
                "Salazar", "Hidalgo", "Velasquez", "Ocampo", "Manalo", "Tolentino", "Magno", "Roxas",
                "Manaloto", "Evangelista", "Delos Santos", "Buenaventura", "Paguirigan", "Camacho",
                "Dela Torre", "Andres", "Espejo", "Ilagan", "Lacson", "Macaraeg", "Ordonez",
                "Sabado", "Tablizo", "Umali", "Vergara", "Yabut", "Zabala", "Abalos", "Bacani",
                "Cabrera", "Encarnacion", "Fajardo", "Galvez", "Hernandez", "Ibarra", "Jacinto",
                "Kapunan", "Laurel", "Macapagal", "Natividad", "Obispo", "Padilla", "Roque",
                "Suarez", "Tadeo", "Ureta", "Vargas", "Alcantara", "Baluyot", "Canlas", "Dayrit",
                "Eugenio", "Fugoso", "Gatchalian", "Halili", "Jimenez", "Lacanilao", "Mabilog",
                "Nograles", "Oreta", "Ponce", "Quimbo", "Ramirez", "Sotto", "Tiangco", "Valdez",
                "Zubiri", "Ablaza", "Bagatsing", "Cayetano", "Defensor", "Estrada", "Fonacier",
                "Guingona", "Honasan", "Imperial", "Jalandoni", "Katigbak", "Lazaro", "Madrigal",
                "Nepomuceno", "Ongpin", "Palma", "Quezon", "Romualdez", "Sumulong", "Tupas",
                "Unson", "Villar", "Wenceslao", "Yulo", "Zobel", "Araneta", "Benedicto", "Cojuangco",
                "Durano", "Enrile", "Gaston", "Herrera", "Ituralde", "Jayme", "Kanlaon", "Llamas",
                "Manotok", "Neri", "Ortigas", "Prieto", "Quirino", "Roces", "Soriquez", "Tinio",
                // Chinese-Filipino
                "Lim", "Tan", "Uy", "Sy", "Co", "Chua", "Ang", "Yu", "Go", "Dee",
                "Dy", "King", "Lee", "Ong", "See", "Tiu", "Wong", "Yao", "Que", "Kho",
                "Cu", "Dycaico", "Limcaoco", "Tancangco", "Uymatiao", "Syquia", "Cobankiat",
                // Regional surnames
                "Alvarado", "Batungbakal", "Calimag", "Dalisay", "Eulalio", "Formoso", "Gabieta",
                "Hapitan", "Inigo", "Jagonap", "Katalbas", "Lumibao", "Magsino", "Narvasa",
                "Oliva", "Palaganas", "Quizon", "Rafanan", "Saguin", "Talavera", "Ulep",
                "Viray", "Wagas", "Yalong", "Zamudio", "Agpalo", "Balleza", "Calimlim",
                "Dagdag", "Estepa", "Faminialagao", "Gabunada", "Hagad", "Ibarrientos",
                "Jularbal", "Kahayon", "Lugtu", "Masangkay", "Nisperos", "Ondevilla",
                "Pagaduan", "Quitain", "Ramiscal", "Sibayan", "Taguiwalo", "Udan", "Valmores"
        };

        String[] genders = {"Male", "Female"};
        String[] civilStatuses = {"Single", "Married", "Widowed", "Separated"};
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        String[] religions = {
                "Roman Catholic", "Iglesia ni Cristo", "Born Again Christian", "Islam",
                "Seventh-day Adventist", "Baptist", "Methodist", "Aglipayan", "Jehovah's Witness",
                "Church of Christ", "Protestant"
        };
        String[] educationalAttainments = {
                "Elementary Graduate", "High School Graduate", "Vocational/Technical",
                "College Graduate", "Post Graduate", "Elementary Undergraduate",
                "High School Undergraduate", "College Undergraduate"
        };
        String[] occupations = {
                "Employee", "Self-employed", "Vendor", "Driver", "Carpenter", "Teacher",
                "Nurse", "Security Guard", "Mechanic", "Farmer", "Housewife", "Laborer",
                "Government Employee", "Electrician", "Plumber", "Cook", "Salesperson",
                "Tricycle Driver", "Construction Worker", "Fisher", "Technician", "Accountant",
                "Engineer", "Lawyer", "Businessman", "OFW", "Student"
        };
        String[] streets = {
                "Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6",
                "Purok 7", "Purok 8", "Sitio Malinis", "Sitio Bagong Buhay",
                "Sitio Maliwanag", "Sitio Payatas", "Sitio Kaunlaran"
        };

        Random random = new Random();
        System.out.println("Seeding " + count + " Filipino Residents...");

        for (int i = 0; i < count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName  = lastNames[random.nextInt(lastNames.length)];
            String middleName = lastNames[random.nextInt(lastNames.length)];
            String street = streets[random.nextInt(streets.length)];

            Person person = new Person();
            person.setFirstName(firstName);
            person.setLastName(lastName);
            person.setMiddleName(middleName);
            person.setGender(genders[random.nextInt(genders.length)]);
            person.setCivilStatus(civilStatuses[random.nextInt(civilStatuses.length)]);
            person.setContactNumber("09" + (100000000 + random.nextInt(900000000)));
            person.setCompleteAddress(street + ", Brgy. Ugong, Valenzuela City, Metro Manila");
            person.setIsResident(true);
            person.setEmail(firstName.toLowerCase().replaceAll("\\s+", "") + i + "@example.com");

            int age;
            if (i < 100) {
                age = 60 + random.nextInt(26);
            } else {
                age = 18 + random.nextInt(42);
            }
            person.setAge((short) age);
            person.setOccupation(age >= 60 ? "Retired" : occupations[random.nextInt(occupations.length)]);
            person.setBirthDate(LocalDate.now().minusYears(age).minusDays(random.nextInt(365)));

            Person savedPerson = personRepository.save(person);

            Resident resident = new Resident();
            resident.setPerson(savedPerson);
            resident.setBarangayIdNumber("2026-BID-" + String.format("%04d", i + 1));
            resident.setHouseholdNumber("2026-HH-" + String.format("%04d", random.nextInt(10000)));

            int num = random.nextInt(10000);
            char letter = (char) ('A' + random.nextInt(4));
            String precinct = random.nextBoolean()
                    ? String.format("%04d-%c", num, letter)
                    : String.format("%04d%c", num, letter);
            resident.setPrecinctNumber(precinct);

            resident.setIsVoter(age >= 18 && (age >= 60 || random.nextDouble() < 0.75));
            resident.setIsHeadOfFamily(random.nextDouble() < 0.25);
            resident.setCitizenship("Filipino");
            resident.setDateOfResidency(
                    LocalDate.now().minusYears(random.nextInt(20)).minusDays(random.nextInt(365))
            );
            resident.setReligion(religions[random.nextInt(religions.length)]);
            resident.setEducationalAttainment(educationalAttainments[random.nextInt(educationalAttainments.length)]);

            // Blood type — 60% may laman, 40% null
            resident.setBloodType(random.nextDouble() < 0.6 ? bloodTypes[random.nextInt(bloodTypes.length)] : null);

            // 4Ps — ~15% chance
            resident.setIs4ps(random.nextDouble() < 0.15);

            // Indigent — ~20% chance
            resident.setIsIndigent(random.nextDouble() < 0.20);


            boolean isPwd = random.nextDouble() < 0.10;
            resident.setIsPwd(isPwd);
            if (isPwd) {
                String pwdId = String.format("13-05-19-%03d-%07d",
                        random.nextInt(900) + 100,
                        i + 1
                );
                resident.setPwdIdNumber(pwdId);
            } else {
                resident.setPwdIdNumber(null);
            }

            // Documents — empty (null sa DB)

            residentRepository.save(resident);
        }

        System.out.println("Successfully seeded " + count + " residents.");
    }






}



