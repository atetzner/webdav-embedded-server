module webdav.embedded.server {
    requires commons.io;
    requires jcommander;
    requires mina.core;
    requires org.apache.commons.lang3;
    requires slf4j.api;

    requires javax.servlet.api;
    requires jetty.server;
    requires milton.api;
    requires milton.server.ce;

    exports de.bitinsomnia.webdav.server;

}
