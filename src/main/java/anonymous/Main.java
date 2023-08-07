package anonymous;

import anonymous.compiler.*;
import anonymous.mult.IMult;
import anonymous.mult.MultFactory;
import anonymous.mult.replicated.ReplicatedMult;
import anonymous.network.DummyNetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.OurProtocol;
import anonymous.rsa.our.RSAUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

import static anonymous.DefaultSecParameters.COMP_SEC;
import static anonymous.DefaultSecParameters.STAT_SEC;

public class Main {
    public static final int WARMUP = 50;
    public static final int ITERATIONS = 100;
    public static final Map<Integer, BigInteger> MMap = new HashMap<>();
    static {
        MMap.put(1024, new BigInteger("7723788480627799417120147169960965855208213704490236863394669743910124816009849493075181146446695284055388077762204796418128227977445467405113575444036201307434005225753120681841575493391610159380581034227134200560181751098676022550109558996268543522661696597962277687800610923062405280297492758432736210533258169022929188088444921182467179640372522078492387116896640841200443276795260363106265985429492318359590617864372475977847359464814094962331960526280101664912030215704338288781169789896646441586401385464603843886337309063423797121623310151865423338831745335513657671878348635101797726026937541015470313401431459"));
        MMap.put(1536, new BigInteger("858170957380331243464323867288638590909385225680585228933073519420308782595717261745325681668604882581875270233664540663719579685820927449882201505499428101528236193173798185304206143791810872234997690905481841168954339060175718906791174457062270916610597965259598060472074981743764305514286618902310303225579958428064766225420694567590048838607113841174638053205527743110442937254699780899453990015410220936837392346165318737049613149237702169374922325303997648621317186743682759890015690632987262303658320344719799939165297290443858775460055624400445048263237136253773894161529201165285437279422759924967653371439935708680315581216951094940186241493765300080665261071242884552185624798988111183478729356835973189799913504582249113483981789732844296455415350364462619789840301173376447283503107642506232704700783059565720377250420118383397834880396644722612414608220733587721865698222758111591709269168390207584619763490121867"));
        MMap.put(2048, new BigInteger("249609719221970614261094767962720433765411102994582158510647377769598645614162195551973313567492601531002128217337864359329791040408330424380561581529195676635391831964702649822271381380276630898248622207308510228045740771381698664254964092243121772300242110227376837038729622973354575334448464230361633476235347678171143098872889053314845907992625320693845358129642164999593261914113024735691457261332118004522842225978883004555395367816102219636625865815755572314117887759657408611079397002613148342208444872035491433799899864308756976554218180315071191697790476560510787101157099565473416822708380387593625727642514797213386371559815732960012061184452295360716230570668956572822730825566742051105286128451460708483096841721831941128951030410092370615864733759667328476771541842444895702534649194119850630986100375363915321642770724267015374483863253750524927225163211680210111492323339127440160727418598977534171201262912049210935223586740697349276057745931368034928348178519165465821547467867084778224075400621934274344213336036260875871384837942604629248788214390727167843635931252458854527049773476569097382038460435235640332628150979401785828692052786296846540837636388400036509386274655982412665621451212552410213731238382366927"));
    }

    public static final Map<Integer, BigInteger> PMap = new HashMap<>();
    static {
        PMap.put(1024, new BigInteger("1977289851040716650782757675510007258933302708349500637029035454440991952898521470227246373490353992718179347907124427883040826362226039655709075313673267534703105337792798894551443326308252200801428744762146355343406528281261061772828047103044747141801394329078343088076956396303975751756158146158780469896514091269869872150641899822711597987935365652094051101925540055347313478859586652955204092269950033500055198173279353850328924022992408310356981894727706026217479735220310601927979466213541489046118754678938584034902351120236492063135567398877548374740926805891496364000857250586060217862896010499960400230766434911"));
        PMap.put(1536, new BigInteger("355456039084882757029575742355832809533139561122088580541389876003937988454116975285011827322982056063345326759239153792996969259991438610793443501808566837475056882701164460613538224206157949294943748406161147978165374269023941869005204156402815071873051023587453922943305546154301218867922112884277459094657327100925472284367250171900811721744130475318578437297394475869254923398106707434685997002254173077298676201872906914814506785201519566066227503644290885911004715103236455469114328683858505666827110859594133578830848684046900709740319893760977530579376621160265367815087232568390049796810478537667517149307344397531708405467649742291764910039011732814190256095728546723884156431303187797915989507732102014926092546322583898295047122389687208302513296156839808897473811879640940104999560570343092888757152190264313352153038167380279922191628590185323869066850021384758845885244551267093464717492680515634772157072085057719"));
        PMap.put(2048, new BigInteger("48902971424492152994450862235742201570946250170280639488097008615153886350560673100409595191199705608595576684610528331447899416951045302123834519576081100438842353379077540126099110482239827505192262144210922515955965306916739729525049856609435140388219849115338693590777467737148882759908775824791373252592035155453525603454512943729422510488833695978723938342108475966458639864521735651069435819379362507662047270921703138794233881480308625858476041214885829087670237287934316590717672431622236462934420704436698879499439312111532252059361705367160217599573337741545203618807788811485887876009607363833056154194305551160401573279799651249718642083288325455685835480204943872612725798607895665333992302800832148016833780970204396837680552410053627246233855232451693024551426101813039517429514386833315314171346213569413233786583697331005662145548729499582497098322134141835792916461590091893497691508093966249337949283715855545689302452159724069864304615917425232588919519394759560987511476910468563472264353514879088061675592779817117380083644217296692035515899657683692818711853851877449544896590705823286753861622853533044998270215717582192061380859388333471271712508576953160313161810279166268090541079362414088881638340554460812103"));
    }

    public static final Map<Integer, BigInteger> QMap = new HashMap<>();
    static {
        QMap.put(1024, new BigInteger("506186201866423462600385964930561858286925493337472163079433076336893939942021496378175071613530622135853913064223853538058451548729866151861523280300356488883994966474956517005169491534912563405165758659109466967912071240002831813843980058379455268301156948244055830547700837453817792449576485416647800293507607365086687270564326354614169084911453606936077082092938254168912250588054183156532247621107208576014130732359514585684204549886056527451387365050292742711674812216399514093562743350666621195806401197808277512935001886780541968162705254112652383933677262308223069184219456150031415772901378687989862459076206453451"));
        QMap.put(1536, new BigInteger("90996746005729985799571390043093199240483727647254676618595808257008125044253945672963027794683406352216403650365223371007224130557808284363121536462993110393614561971498101917065785396776435019505599591977253882410335812870129118465332264039120658399501062038388204273486219815501112030188060898375029528232275737836920904798016044006607800766497401681556079948132985822529260389915317103279615232577068307788461107679464170192513737011589008912954240932938466793217207066428532600093268143067777450707740380056098196180697263116006581693521892802810247828320415017027934160662331537507852747983482505642884390222680165768117351799718334026691816969987003600432705560506507961314344046413616076266493313979418115821079691858581477963532063331759925325443403816150991077753295841188080666879887506007831779521830960707664218151177770849351660081056919087442910481113605474498264546622605124375926967678126212002501672210453774276319"));
        QMap.put(2048, new BigInteger("16358422558931066176215106713204846347249982045852936340153786549508416838969733647694123077959195133935755474851454278653037185624200342692204483807097367863977039099638752858752377250137809282547621704978170526305205667193271003660613326749245228469468666935861368392170184571181765449118414551800980011498559745436624034127733656998041741426204693016991849390384228925413344012803311189078275343078661685544408988121752076586542390825196075066105912742101357187178029892216907930735699361963255289754972643133717966605510237507338697215457242665128505619106396671869634943461431677122865844893016417081335855686779849750176489246544083875267350441784265628759898886679360737956510487384342007061236031714194928991148234619081978093826934728955813600681311191673558039053699766186468684761310769585838530952305074199849554519180622185563119582174462197794401630628296240674249866760902353055918373432105302591679443845966204057087850812980638341482155720437358135537064226227432027968080934854137268025693005455159084603422365190472392761107076739406536982448584418310695671800524390561143490284733954560432366029272543083602924839118502586075436069162371402750134900335338350184792679138895854463396454167426665834755767090438626461373927"));
    }

    public static final NetworkFactory.NetworkType NETWORK_TYPE = NetworkFactory.NetworkType.DUMMY;
    public static void main(String[] args) throws Exception {
        System.out.println("COMPILER");
        benchCompiler(2048);
        benchCompiler(2048);
        benchCompiler(3072);
        benchCompiler(4096);
        System.out.println("POST");
        postprocessing(2048);
        postprocessing(2048);
        postprocessing(3072);
        postprocessing(4096);
    }

    public static void benchCompiler(int bitlength) throws Exception {
        System.out.println(bitlength);
        Random rand = new Random(42);
        int parties = 3;
        Map<Integer, BigInteger> pShares = randomPrime(parties, bitlength/2, rand);
        Map<Integer, BigInteger> qShares = randomPrime(parties, bitlength/2, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        // NOTE that we need copies of both, since the network for mult is stored in the parameters!!! Terrible decision! TODO FIX!
        Map<Integer, OurParameters> brainParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                OurProtocol.MembershipProtocol membership = OurProtocol.MembershipProtocol.LOG;
                CompiledProtocolResources resources = new CompiledProtocolResources(COMP_SEC);
                BigInteger temp = BigInteger.ZERO;
                for (int j = 0; j < WARMUP; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    protocol.init(networks.get(finalI), getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    // ensure things get run
                    temp = temp.add(localRes);
                }
                List<Long> times = new ArrayList<>();
                Field privateField = ReplicatedMult.class.getDeclaredField("network");
                privateField.setAccessible(true);
                ((DummyNetwork) ((PinkyNetwork) privateField.get(pinkyParameters.get(finalI).getMult())).internalNetwork).resetCount();
                long sum = 0l;
                for (int j = 0; j < ITERATIONS; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    long start = System.nanoTime();
                    protocol.init(networks.get(finalI), getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    long stop = System.nanoTime();
                    // ensure things get run
                    temp = temp.add(localRes);
                    sum += (stop-start)/1000;
                    times.add((stop-start)/1000);
                }
                long avg = sum/ITERATIONS;
                long stdSum = 0l;
                for (long cur : times) {
                    stdSum += (cur-avg)*(cur-avg);
                }
                double std = Math.sqrt(stdSum/ITERATIONS);
                System.out.println("sender " + avg + " std " + std);
                return BigInteger.ZERO ;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(20000, TimeUnit.SECONDS);

//        //TODO ensure correctness of pinky com! Result should 1!
//        for (Future<BigInteger> cur : res) {
//            assertEquals(BigInteger.ONE, cur.get());
//        }
//        runProtocolTest(brainNets, pinkyNets, parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
        System.out.println("Nettime " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getNetworkTime())
                +((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getNetworkTime())/ITERATIONS);
        System.out.println("Net bytes " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getBytesSent()
                + ((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getBytesSent())/ITERATIONS));
    }

    public static void postprocessing(int bitlength) throws Exception {
        System.out.println(bitlength);
        int parties = 3;
        Random rand = new Random(41);
        BigInteger modulo =  DefaultSecParameters.findMaxPrime(bitlength+4);
        BigInteger a = RSAUtil.sample(rand, modulo);
        BigInteger b = RSAUtil.sample(rand, modulo);
        BigInteger c = a.multiply(b).mod(modulo);
        Map<Integer, BigInteger> aShares = share(a, parties, modulo, rand);
        Map<Integer, BigInteger> bShares = share(b, parties, modulo, rand);
        Map<Integer, BigInteger> cShares = share(c, parties, modulo, rand);

        Map<Integer, OurParameters> sharingParams = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> brainParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Boolean>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                PostprocessingProtocol protocol = new PostprocessingProtocol(brainParameters.get(finalI), pinkyParameters.get(finalI), COMP_SEC);
                protocol.init(networks.get(finalI), getRandom(networks.get(finalI).getBrainNetwork().myId()));
                sharingParams.get(finalI).getMult().init(networks.get(finalI).getBrainNetwork().internalNetwork, getRandom(networks.get(finalI).getBrainNetwork().myId()));
                Serializable myAShare = sharingParams.get(finalI).getMult().shareFromAdditive(aShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myBShare = sharingParams.get(finalI).getMult().shareFromAdditive(bShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myCShare = sharingParams.get(finalI).getMult().shareFromAdditive(cShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                boolean curRes = true;
                for (int j = 0; j < WARMUP; j++) {
                    curRes ^= protocol.execute(Arrays.asList(new PostprocessingProtocol.Multiplication(myAShare, myBShare, myCShare, modulo)));
                }
                Field privateField = ReplicatedMult.class.getDeclaredField("network");
                privateField.setAccessible(true);
                ((DummyNetwork) ((PinkyNetwork) privateField.get(pinkyParameters.get(finalI).getMult())).internalNetwork).resetCount();
                List<Long> times = new ArrayList<>();
                long sum = 0l;
                for (int j = 0; j < ITERATIONS; j++) {
                    long start = System.nanoTime();
                    for (int k = 0; k < 27; k++) {
                        curRes ^= protocol.execute(Arrays.asList(new PostprocessingProtocol.Multiplication(myAShare, myBShare, myCShare, modulo)));
                    }
                    long stop = System.nanoTime();
                    sum += (stop-start)/1000;
                    times.add((stop-start)/1000);
                }
                long avg = sum/ITERATIONS;
                long stdSum = 0l;
                for (long cur : times) {
                    stdSum += (cur-avg)*(cur-avg);
                }
                double std = Math.sqrt(stdSum/ITERATIONS);
                System.out.println("sender " + avg + " std " + std);
                return curRes;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(20000, TimeUnit.SECONDS);

        System.out.println("Nettime " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getNetworkTime())
                +((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getNetworkTime())/ITERATIONS);
        System.out.println("Net bytes " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getBytesSent()
                + ((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getBytesSent())/ITERATIONS));
    }

    public static Random getRandom(int myId) {
//        SecureRandom random = ExceptionConverter.safe( ()-> SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not get random");
//        random.setSeed(myId);
//        return random;
        return new SecureRandom();
    }

    public static Map<Integer, BigInteger> randomPrime(int parties, int bitLength, Random rand) {
        BigInteger prime = prime(bitLength, rand);
        Map<Integer, BigInteger> shares = new HashMap<>(parties);
        // We sample a number small enough to avoid issues with negative shares
        for (int party = 1; party < parties; party++) {
            shares.put(party, (new BigInteger(bitLength - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
        }
        shares.put(0, prime.subtract(shares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        return shares;
    }

    public static Map<Integer, OurParameters> getOurParameters(int bits, int statSec, int parties, boolean decorated, MultFactory.MultType multType) {
        try {
            // TODO the 8 increments are needed for OT mult protocols but not others
            BigInteger M;
            BigInteger P;
            BigInteger Q;
            if (bits == 1024 || bits == 1536 || bits == 2048) {
                M = MMap.get(bits);
                P = PMap.get(bits);
                Q = QMap.get(bits);
            } else {
                // M > 2^(2*bits)
                M = prime(2 * bits + 8, new Random(41));
                // P > mN, we assume at most 2048 parties
                P = prime(2 * bits + 16, new Random(41));
                // Q > P
                Q = prime(2 * bits + 24, new Random(41));
            }
            MultFactory multFactory = new MultFactory(parties);
            Map<Integer, OurParameters> params = new HashMap<>(parties);
            Map<Integer, IMult> mults = multFactory.getMults(multType, NETWORK_TYPE, decorated);
            for (int i = 0; i < parties; i++) {
                // Unique but deterministic seed for each set of parameters
                params.put(i, new OurParameters(bits, statSec, P, Q, M, mults.get(i)));
            }
            return params;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static BigInteger prime(int bits, Random rand) {
        BigInteger cand;
        do {
            cand = BigInteger.probablePrime(bits, rand);
        } while (!cand.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)));
        return cand;
    }

    public static Map<Integer, BigInteger> share(BigInteger value, int parties, BigInteger modulus, Random rand) {
        Map<Integer, BigInteger> shares = new ConcurrentHashMap<>(parties);
        BigInteger sum = BigInteger.ZERO;
        for (int i = 1; i < parties; i++) {
            BigInteger randomNumber = new BigInteger(modulus.bitLength(), rand);
            sum = sum.add(randomNumber);
            shares.put(i, randomNumber);
        }
        // Compute pivot
        shares.put(0, value.subtract(sum).mod(modulus));
        return shares;
    }
}