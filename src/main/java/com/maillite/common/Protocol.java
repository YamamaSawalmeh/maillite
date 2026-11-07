package com.maillite.common;

public class Protocol {
    // Commands
    public static final String CMD_HELO = "HELO";
    public static final String CMD_AUTH = "AUTH";
    public static final String CMD_SETSTAT = "SETSTAT";
    public static final String CMD_WHO = "WHO";
    public static final String CMD_LIST = "LIST";
    public static final String CMD_RETR = "RETR";
    public static final String CMD_SEND = "SEND";
    public static final String CMD_DELE = "DELE";
    public static final String CMD_RESTORE = "RESTORE";
    public static final String CMD_STAT = "STAT";
    public static final String CMD_QUIT = "QUIT";

    // Responses
    public static final String RESP_READY = "250 READY";
    public static final String RESP_OK = "250 OK";
    public static final String RESP_AUTH_OK = "235 OK";
    public static final String RESP_AUTH_FAIL = "535 AUTH FAILED";
    public static final String RESP_BYE = "221 BYE";
    public static final String RESP_UNKNOWN = "500 UNKNOWN COMMAND";
    public static final String RESP_BAD_ARGS = "501 BAD ARGUMENTS";
    public static final String RESP_SEND_START = "354 FROM? TO? SUBJECT? BODYLEN?";
    public static final String RESP_SEND_BODY = "354 BODY";
    
    // Status
    public static final String STAT_ACTIVE = "ACTIVE";
    public static final String STAT_BUSY = "BUSY";
    public static final String STAT_AWAY = "AWAY";

    // UDP
    public static final String UDP_NOTIFY = "NOTIFY NEWMAIL";
}
