package yangFenTuoZi.shamikox.server;

import yangFenTuoZi.shamikox.server.magisk.SuPolicy;

interface IService {
    void stopServer();

    int testConnect();

    String getVersionName();
    int getVersionCode();

    void delete(in SuPolicy suPolicy);
    void update(in SuPolicy suPolicy);
    void insert(in SuPolicy suPolicy);
    SuPolicy queryById(int uid);
    SuPolicy[] queryAll();
    boolean exist(int uid);
    void insertOrUpdate(in SuPolicy suPolicy);

    boolean isWhitelist();
    boolean change(boolean whitelist);
    void authorize(int uid);
    void authorizeForegroundApp();
}