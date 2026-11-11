package ua.edu.digitaldean.app;

import java.util.ArrayList;
import java.util.List;

import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.service.DelimitedRequestFactory;
import ua.edu.digitaldean.service.DiscountFeePolicy;
import ua.edu.digitaldean.service.FeePolicy;
import ua.edu.digitaldean.service.ProcessingResult;
import ua.edu.digitaldean.service.RequestFactory;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class DigitalDeanApp {
    private static final String USAGE =
            "Використання: DigitalDeanApp <studentId> "
                    + "<discountPercent> <requestSpec>...";

    private DigitalDeanApp() {
    }

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println(USAGE);
                return;
            }

            StudentId studentId = new StudentId(args[0]);
            int discountPercent = Integer.parseInt(args[1]);

            RequestFactory factory = new DelimitedRequestFactory();
            FeePolicy basePolicy = new StandardFeePolicy();

            FeePolicy discountPolicy =
                    new DiscountFeePolicy(basePolicy, discountPercent);

            RequestProcessor processor =
                    new RequestProcessor(discountPolicy);

            List<AcademicRequest> requests = new ArrayList<>();

            for (int index = 2; index < args.length; index++) {
                requests.add(factory.create(studentId, args[index]));
            }

            List<ProcessingResult> results =
                    processor.processAll(requests);

            for (ProcessingResult result : results) {
                System.out.println(result.formattedLine());
            }

            System.out.println("Опрацьовано заяв: " + results.size());
            System.out.println(
                    "Загальна сума зборів: "
                            + processor.totalFee(results)
                            .toPlainString()
                            + " UAH");

        } catch (NumberFormatException exception) {
            System.err.println(
                    "Помилка: Відсоток знижки, кількість примірників "
                            + "і місяців мають бути цілими числами.");
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: " + exception.getMessage());
        }
    }
}