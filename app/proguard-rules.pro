# Zachowanie modeli danych SchoolSpace przed obfuskacją (wymagane dla Firebase Firestore)
-keep class com.example.schoolspace.Lesson { *; }
-keep class com.example.schoolspace.Grade { *; }
-keep class com.example.schoolspace.Message { *; }
-keep class com.example.schoolspace.ScheduleChange { *; }
-keep class com.example.schoolspace.Reservation { *; }

# Firebase Firestore wymaga zachowania nazw pol dla serializacji
-keepclassmembers class com.example.schoolspace.** {
    <fields>;
    <methods>;
}

# Ogólne reguły dla Firebase
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
