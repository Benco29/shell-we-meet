package benco.shellwemeet.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

public class DateValidator {

    private final String TAG = "DateValidator";

    public boolean isDateValid(String inputDate, String dateFormat) {

        if (inputDate == null || inputDate.isEmpty()) {
            return false;
        }
        if (dateFormat == null || dateFormat.isEmpty()) {
            return false;
        } else {

            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            format.setLenient(false);

            try {
                format.parse(inputDate);
            } catch (ParseException pe) {
                return false;
            }
        }
        return true;
    }

    public int ageCalculator(String date /*, DateFormat dateFormat*/) {

        int age = 1;

        int day, month, year;

        Calendar now = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();

        int[] dateComponents = intArrayFromStringDate(date);

        day = dateComponents[0];
        month = dateComponents[1];
        year = dateComponents[2];

        dob.set(year, month, day);

        age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (dob.after(now)) {
            throw new IllegalArgumentException("Can't be born in the future");
        }

        int month1 = now.get(Calendar.MONTH);
        int month2 = dob.get(Calendar.MONTH);
        if (month2 > month1) {
            age--;
        } else if (month1 == month2) {
            int day1 = now.get(Calendar.DAY_OF_MONTH);
            int day2 = dob.get(Calendar.DAY_OF_MONTH);
            if (day2 > day1) {
                age--;
            }
        }

        return age;
    }

    private int[] intArrayFromStringDate(String date) {

        int day = 0, month = 0, year = 0;

        if (date.contains(".")) {
            String delim = ".";
            String[] dateComp = date.split(Pattern.quote(delim));
            day = Integer.parseInt(dateComp[0]);
            month = Integer.parseInt(dateComp[1]);
            year = Integer.parseInt(dateComp[2]);

        } else if (date.contains("/")) {
            String delim = "/";
            String[] dateComp = date.split(Pattern.quote(delim));
            day = Integer.parseInt(dateComp[0]);
            month = Integer.parseInt(dateComp[1]);
            year = Integer.parseInt(dateComp[2]);
        }

        return new int[]{day, month, year};
    }

    public static String getNestedString(String str, String open, String close) {
        return substringBetween(str, open, close);
    }

    private static String substringBetween(String str, String open, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != Constants.INDEX_NOT_FOUND) {
            int end = str.indexOf(close, start + open.length());
            if (end != Constants.INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }
}
