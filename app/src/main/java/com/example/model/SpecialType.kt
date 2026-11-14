package com.example.model

enum class SpecialType(val arabicName: String, val description: String) {
    NONE("عادي", "فاكهة طبيعية"),
    STRIPED_HORIZONTAL("شعاع أفقي", "ينظف صفاً كاملاً بأشعة الشمس"),
    STRIPED_VERTICAL("شعاع عمودي", "ينظف عموداً كاملاً بأشعة الشمس"),
    BOMB("قنبلة استوائية", "تنفجر في مساحة 3x3 بتدفق عصائر منعشة"),
    STARFRUIT_RAINBOW("فاكهة النجمة الكونية", "تسحق كل الفواكه من نفس اللون في اللوحة!")
}
