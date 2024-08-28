package com.fromimport.chatgptweb.aspect;

public class PersonalInfoPatterns {

    // 匹配身份证号
    public static final String ID_CARD_PATTERN = "\\d{15}(\\d{2}[0-9xX])?";

    // 匹配手机号
    public static final String PHONE_NUMBER_PATTERN = "\\b(1[3-9]\\d{9})\\b";

    // 匹配邮箱地址
    public static final String EMAIL_PATTERN = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";

    // 匹配银行卡号
    public static final String BANK_CARD_PATTERN = "\\b\\d{16,19}\\b";
}
