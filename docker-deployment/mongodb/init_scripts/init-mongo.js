// ====================================================================
// A8S Cloud Platform: MongoDB Database Initialization
// (Spring Boot @Document will auto-create collections/tables as needed)
// ====================================================================

const databases = [
    'a8s_security_db',
    'a8s_audit_db',
    'a8s_ai_db',
    'a8s_terminal_db'
];

databases.forEach(dbName => {
    const d = db.getSiblingDB(dbName);
    // MongoDB requires at least 1 collection to allocate the database on disk.
    // Spring Boot's @Document annotations will create all real collections/tables.
    d.createCollection('_init');
    print('Initialized database: ' + dbName);
});

print('====================================================');
print('All 4 A8S databases initialized. Spring will handle collections!');
print('====================================================');
