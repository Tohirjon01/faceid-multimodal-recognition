package uz.tuit.faceid.recognition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.tuit.faceid.recognition.config.properties.DemoProperties;
import uz.tuit.faceid.recognition.config.properties.MatchingProperties;
import uz.tuit.faceid.recognition.dto.response.SeedResponse;
import uz.tuit.faceid.recognition.entity.DemoProbe;
import uz.tuit.faceid.recognition.entity.FaceTemplate;
import uz.tuit.faceid.recognition.entity.Subject;
import uz.tuit.faceid.recognition.entity.enums.Expression;
import uz.tuit.faceid.recognition.exception.EmptyGalleryException;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.DemoDataService;
import uz.tuit.faceid.recognition.util.VectorUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DemoDataServiceImpl implements DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataServiceImpl.class);

    /** Fixed seed, so every start produces the same demo data. */
    private static final long SEED_RANDOM_SEED = 42L;

    private static final List<String> FIRST_NAMES = List.of(
            "Tohirjon", "Aziza", "Bekzod", "Dilnoza", "Eldor", "Feruza", "G'ayrat", "Hulkar",
            "Ilhom", "Jasur", "Kamola", "Lola", "Mirzaahmad", "Nodira", "Otabek", "Parviz",
            "Qudrat", "Rustam", "Sevara", "Temur", "Umida", "Vohid", "Xurshid", "Yulduz",
            "Zafar", "Shahzod", "Nargiza", "Doniyor", "Malika", "Sardor");

    private static final List<String> LAST_NAMES = List.of(
            "Sadullayev", "Karimova", "Yusupov", "Tosheva", "Alimov", "Rahimova", "Nazarov",
            "Qodirova", "Mirzayev", "Ergasheva", "Sultonov", "Islomova", "Kamilov", "Saidova",
            "Jo'rayev", "Hamidov", "Abdullayeva", "Normatov", "Tursunova", "Bekmurodov",
            "Mahmudova", "Oripov", "Soliyeva", "Xolmatov", "Ismoilova", "G'aniyev",
            "Nurmatova", "Raufov", "Zokirova", "Akbarov");

    private static final List<String> ROLES = List.of("employee", "manager", "guest", "security");

    private static final List<String> ALL_MODALITIES = List.of("rgb", "depth", "ir");

    private final TemplateRepository templateRepository;
    private final MatchingProperties matchingProperties;
    private final DemoProperties demoProperties;

    public DemoDataServiceImpl(TemplateRepository templateRepository,
                               MatchingProperties matchingProperties,
                               DemoProperties demoProperties) {
        this.templateRepository = templateRepository;
        this.matchingProperties = matchingProperties;
        this.demoProperties = demoProperties;
    }

    @Override
    public SeedResponse seedIfEmpty() {
        if (templateRepository.countTemplates() > 0) {
            log.info("Gallery already holds {} templates — seeding skipped",
                    templateRepository.countTemplates());
            return new SeedResponse(false,
                    templateRepository.countSubjects(), templateRepository.countTemplates());
        }
        return seed(demoProperties.getSubjects(), demoProperties.getTemplatesPerSubject());
    }

    @Override
    public SeedResponse seed(int subjectCount, int templatesPerSubject) {
        Random random = new Random(SEED_RANDOM_SEED);

        for (int index = 0; index < subjectCount; index++) {
            String fullName = "%s %s.".formatted(
                    LAST_NAMES.get(index % LAST_NAMES.size()),
                    FIRST_NAMES.get(index % FIRST_NAMES.size()).charAt(0));
            Subject subject = templateRepository.createSubject(
                    fullName, ROLES.get(index % ROLES.size()));

            // The subject's "true" vector. Each template is a slightly perturbed
            // copy of it, standing in for frames taken under different poses
            // and lighting.
            float[] identityVector = VectorUtils.randomUnit(
                    matchingProperties.getEmbeddingDimension(), random);

            for (int templateIndex = 0; templateIndex < templatesPerSubject; templateIndex++) {
                float[] embedding = templateIndex == 0
                        ? identityVector
                        : VectorUtils.perturb(identityVector,
                                demoProperties.getGenuineNoise() / 2, random);

                templateRepository.saveTemplate(new FaceTemplate(
                        null,
                        subject.id(),
                        matchingProperties.getActiveModelId(),
                        embedding,
                        0.80 + random.nextDouble() * 0.19,
                        ALL_MODALITIES,
                        "minio://faceid/enroll/%s/%d.png".formatted(subject.id(), templateIndex)
                ));
            }
        }

        SeedResponse response = new SeedResponse(true,
                templateRepository.countSubjects(), templateRepository.countTemplates());
        log.info("Demo seed complete: {} subjects, {} templates",
                response.subjects(), response.templates());
        return response;
    }

    @Override
    public DemoProbe createKnownProbe() {
        List<Subject> subjects = templateRepository.findAllSubjects();
        if (subjects.isEmpty()) {
            throw new EmptyGalleryException();
        }

        Subject picked = subjects.get(ThreadLocalRandom.current().nextInt(subjects.size()));
        float[] baseEmbedding = templateRepository.findAnyEmbeddingBySubject(picked.id())
                .orElseThrow(EmptyGalleryException::new);

        float[] probe = VectorUtils.perturb(baseEmbedding, demoProperties.getGenuineNoise(),
                new Random(ThreadLocalRandom.current().nextLong()));
        return new DemoProbe(picked.id(), picked.fullName(), probe);
    }

    @Override
    public DemoProbe createStrangerProbe() {
        float[] probe = VectorUtils.randomUnit(
                matchingProperties.getEmbeddingDimension(),
                new Random(ThreadLocalRandom.current().nextLong()));
        return new DemoProbe(null, null, probe);
    }

    @Override
    public float[] sampleExpressionProbabilities() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float[] probabilities = new float[Expression.COUNT];
        int dominant = random.nextInt(Expression.COUNT);

        double total = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = (float) (i == dominant
                    ? 3.0 + random.nextDouble() * 2.0
                    : random.nextDouble() * 0.4);
            total += probabilities[i];
        }
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = (float) (probabilities[i] / total);
        }
        return probabilities;
    }

    @Override
    public double getSpoofLivenessScore() {
        return demoProperties.getSpoofLiveness();
    }
}
