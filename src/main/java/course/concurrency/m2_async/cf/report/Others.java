package course.concurrency.m2_async.cf.report;

import lombok.Getter;

public class Others {

    static class Item {}
    static class Customer {}
    public static class Report {
        @Getter
        boolean isEmpty;

        public Report(boolean isEmpty) {
            this.isEmpty = isEmpty;
        }


        public Report() {
            this.isEmpty = false;
        }
    }
}
