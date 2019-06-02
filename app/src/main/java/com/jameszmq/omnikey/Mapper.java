package com.jameszmq.omnikey;

public class Mapper {
    public static String map(String id, boolean enter) {
        if (enter) {
            switch (id) {
                case "1": return "A";
                case "2": return "B";
                case "3": return "C";
                case "4": return "D";
                case "5": return "E";
            }
        }
        else {
            switch (id) {
                case "1": return "a";
                case "2": return "b";
                case "3": return "c";
                case "4": return "d";
                case "5": return "e";
            }
        }
        return "";
    }
}
