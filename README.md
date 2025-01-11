## ShamikoX

可以应用内或快速设置页面快速开关Shamiko白名单模式，并可以为前台app申请root权限

不多次开启su会话，使用一个常驻server进行root操作，减少部分开销

## 下载

[Github](https://github.com/yangFenTuoZi/ShamikoX/releases)

## tips

server.cpp socket部分代码是AI的

应用在后台时，server与daemon可能会被Magisk杀死，推荐使用[Runner](https://github.com/yangFenTuoZi/Runner)
或[ShizukuRunner](https://github.com/WuDi-ZhanShen/ShizukuRunner)或Magisk模块启动server

部分国产UI/OS可能需要设置 电池使用无限制、电池优化白名单 才能正常进行socket通信
