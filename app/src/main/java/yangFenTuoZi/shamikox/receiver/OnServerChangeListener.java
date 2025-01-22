package yangFenTuoZi.shamikox.receiver;

import yangFenTuoZi.shamikox.server.IService;

public interface OnServerChangeListener {
    void serverRun(IService iService);
    void serverStop();
}
