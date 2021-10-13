package ai.ecma.paynet.endpoint;

import ai.ecma.paynet.entity.Client;
import ai.ecma.paynet.entity.Payment;
import ai.ecma.paynet.entity.PaynetTransaction;
import ai.ecma.paynet.repository.PaymentRepository;
import ai.ecma.paynet.repository.PaynetTransactionRepository;
import ai.ecma.paynet.repository.ClientRepository;
import ai.ecma.paynet.soap.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.sql.Timestamp;
import java.util.*;

@Endpoint
public class WebserviceEndpoint {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PaynetTransactionRepository paynetTransactionRepository;
    @Autowired
    private PaymentRepository paymentRepository;


    private static final String NAMESPACE_URI = "http://uws.provider.com/";

    //TO'LOV QILISH UCHUN HAMMASI SUCCESS
    @Transactional
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "PerformTransactionArguments")
    @ResponsePayload
    public JAXBElement<PerformTransactionResult> performTransaction(@RequestPayload JAXBElement<PerformTransactionArguments> request) throws DatatypeConfigurationException {
        PerformTransactionArguments requestValue = request.getValue();
        PerformTransactionResult result = new PerformTransactionResult();
        ObjectFactory factory = new ObjectFactory();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(requestValue.getUsername(), requestValue.getPassword());
        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createPerformTransactionResult(result);
        }

        //BU PAYNET BILAN KELISHILIB TO'LOV HAR XIL NARSA UCHUN TO'LANSA KERAK BO'LADI. MISOL UCHUN SIZDA ONLINE UCHUN ALOHIDA,
        // OFFLINE UCHUN ALOHIDA, BOOTCAMP UCHUN ALOHIDA BO'LSA, PAYNET BILAN KELISHIB MIJOZ TANALAGAN TUR UCHUN TO'LOV QILISH UCHUN KERAK
        // (BIZNI HOLATDA TO'LOV FAQAT ID FAQAT 1,2 YOKI 3 SERVICE ID BO'LISHI MUMKIN)
        if (requestValue.getServiceId() != 1 && requestValue.getServiceId() != 2 && requestValue.getServiceId() != 3) {
            result.setErrorMsg("Услуга не найдена");
            result.setStatus(305);
        } else {
            String phoneNumber = "";
            for (GenericParam parameter : requestValue.getParameters()) {
                if (parameter.getParamKey().equals("phone_number")) {
                    phoneNumber = parameter.getParamValue().trim();
                    if (!phoneNumber.startsWith("+")) {
                        phoneNumber = "+" + phoneNumber;
                    }
                }
            }
            try {
                if (!phoneNumber.isEmpty()) {
                    if (paynetTransactionRepository.existsByTransactionId(requestValue.getTransactionId())) {
                        result.setErrorMsg("Транзакция уже существует");
                        result.setStatus(201);
                    } else {
                        Optional<Client> optionalUser = clientRepository.findByUsername(phoneNumber);
                        if (optionalUser.isPresent()) {
                            paynetTransactionRepository.save(new PaynetTransaction(
                                    requestValue.getAmount(),
                                    requestValue.getServiceId(),
                                    requestValue.getTransactionId(),
                                    new Timestamp(requestValue.getTransactionTime().toGregorianCalendar().getTimeInMillis()),
                                    phoneNumber,
                                    1));


                            Payment payment = new Payment(
                                    optionalUser.get(),
                                    (double) requestValue.getAmount() / 100,
                                    (double) requestValue.getAmount() / 100,
                                    requestValue.getTransactionId());

                            paymentRepository.save(payment);

                            result.setErrorMsg("Success");
                            result.setStatus(0);
                        } else {
                            result.setErrorMsg("Клиент не найден");
                            result.setStatus(302);
                        }
                    }
                } else {
                    result.setErrorMsg("Клиент не найден");
                    result.setStatus(302);
                }
            } catch (Exception e) {
//                log.error(e.getMessage());
                e.printStackTrace();
                result.setErrorMsg("Клиент не найден");
                result.setStatus(302);
            }
        }
        result.setProviderTrnId(requestValue.getTransactionId());
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        return factory.createPerformTransactionResult(result);
    }


    //PUL TUSHGANDAN SO'NG PAYNET TEKSHIRISH UCHUN KELADI
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CheckTransactionArguments")
    @ResponsePayload
    public JAXBElement<CheckTransactionResult> checkTransaction(@RequestPayload JAXBElement<CheckTransactionArguments> request) throws DatatypeConfigurationException {
        ObjectFactory factory = new ObjectFactory();
        CheckTransactionArguments requestValue = request.getValue();
        CheckTransactionResult result = new CheckTransactionResult();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(requestValue.getUsername(), requestValue.getPassword());
        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createCheckTransactionResult(result);
        }
        GregorianCalendar c = new GregorianCalendar();

        //BU PAYNET BILAN KELISHILIB TO'LOV HAR XIL NARSA UCHUN TO'LANSA KERAK BO'LADI. MISOL UCHUN SIZDA ONLINE UCHUN ALOHIDA,
        // OFFLINE UCHUN ALOHIDA, BOOTCAMP UCHUN ALOHIDA BO'LSA, PAYNET BILAN KELISHIB MIJOZ TANALAGAN TUR UCHUN TO'LOV QILISH UCHUN KERAK
        // (BIZNI HOLATDA TO'LOV FAQAT ID FAQAT 1,2 YOKI 3 SERVICE ID BO'LISHI MUMKIN)
        if (requestValue.getServiceId() != 1 && requestValue.getServiceId() != 2 && requestValue.getServiceId() != 3) {
            result.setErrorMsg("Услуга не найдена");
            result.setStatus(305);
            c.setTime(new Date());
        } else {
            Optional<PaynetTransaction> transaction = paynetTransactionRepository.findByTransactionId(requestValue.getTransactionId());
            if (transaction.isPresent()) {
                if (transaction.get().getState() == 2) {
                    result.setErrorMsg("Транзакция уже отменена");
                    result.setStatus(202);
                    result.setTransactionState(transaction.get().getState());
                    result.setTransactionStateErrorStatus(202);
                    result.setTransactionStateErrorMsg("Транзакция уже отменена");
                } else {
                    result.setStatus(0);
                    result.setErrorMsg("Success");
                    c.setTimeInMillis(transaction.get().getTransactionTime().getTime());
                    result.setProviderTrnId(transaction.get().getTransactionId());
                    result.setTransactionState(transaction.get().getState());
                    result.setTransactionStateErrorMsg("Success");
                    result.setTransactionStateErrorStatus(0);
                }
            } else {
                result.setErrorMsg("Транзакция не существует");
                result.setStatus(102);
                result.setTransactionStateErrorMsg("Транзакция не существует");
                result.setTransactionStateErrorStatus(102);
            }
        }
        result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        return factory.createCheckTransactionResult(result);
    }


    //TO'LOVNI BEKOR QILISH
    @Transactional
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CancelTransactionArguments")
    @ResponsePayload
    public JAXBElement<CancelTransactionResult> cancelTransaction(@RequestPayload JAXBElement<CancelTransactionArguments> request) throws DatatypeConfigurationException {
        ObjectFactory factory = new ObjectFactory();
        CancelTransactionArguments requestValue = request.getValue();
        CancelTransactionResult result = new CancelTransactionResult();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(requestValue.getUsername(), requestValue.getPassword());

        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createCancelTransactionResult(result);
        }
        GregorianCalendar c = new GregorianCalendar();

        //BU PAYNET BILAN KELISHILIB TO'LOV HAR XIL NARSA UCHUN TO'LANSA KERAK BO'LADI. MISOL UCHUN SIZDA ONLINE UCHUN ALOHIDA,
        // OFFLINE UCHUN ALOHIDA, BOOTCAMP UCHUN ALOHIDA BO'LSA, PAYNET BILAN KELISHIB MIJOZ TANALAGAN TUR UCHUN TO'LOV QILISH UCHUN KERAK
        // (BIZNI HOLATDA TO'LOV FAQAT ID FAQAT 1,2 YOKI 3 SERVICE ID BO'LISHI MUMKIN)
        if (requestValue.getServiceId() != 1 && requestValue.getServiceId() != 2 && requestValue.getServiceId() != 3) {
            result.setErrorMsg("Услуга не найдена");
            result.setStatus(305);
            c.setTime(new Date());
        } else {
            Optional<PaynetTransaction> optionalPaynetTransaction = paynetTransactionRepository.findByTransactionId(requestValue.getTransactionId());
            if (optionalPaynetTransaction.isPresent()) {

                PaynetTransaction paynetTransaction = optionalPaynetTransaction.get();

                //PAYNET TRANSACTION NING STATE 2 BO'LSA TRANSACTION CANCEL QILINGAN DEGANI
                if (paynetTransaction.getState() == 2) {
                    result.setErrorMsg("Транзакция уже отменена");
                    result.setStatus(202);
                    result.setTransactionState(paynetTransaction.getState());
                    c.setTime(new Date());
                } else {

                }
                Optional<Payment> optionalPayment = paymentRepository.findFirstByTransactionIdOrderByPayDateDesc(paynetTransaction.getId());
                if (optionalPayment.isPresent()) {
                    Payment payment = optionalPayment.get();
                    if (payment.getPaySum().equals(payment.getLeftoverSum())) {
                        paynetTransaction.setState(2);
                        paynetTransactionRepository.save(paynetTransaction);
                        payment.setLeftoverSum(0D);
                        payment.setCancelled(true);
                        paymentRepository.save(payment);

                        result.setErrorMsg("Success");
                        result.setStatus(0);
                        c.setTimeInMillis(paynetTransaction.getTransactionTime().getTime());
                        result.setTransactionState(2);
                    } else {
                        result.setErrorMsg("You can't cancel this transaction");
                        result.setStatus(401);
                        c.setTime(new Date());
                    }
                }
            } else {
                result.setErrorMsg("Транзакция не существует");
                result.setStatus(102);
                c.setTime(new Date());
            }
        }

        result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));

        return factory.createCancelTransactionResult(result);
    }


    //PAYNET BIZ ORQALI QAYSIDIR VAQT ORALIG'IDA QILINGNA TRANSACTION LAR LISTINI SO'RAYDI
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetStatementArguments")
    @ResponsePayload
    public JAXBElement<GetStatementResult> getStatement(@RequestPayload JAXBElement<GetStatementArguments> request) throws DatatypeConfigurationException {
        ObjectFactory factory = new ObjectFactory();
        GetStatementArguments requestValue = request.getValue();
        GetStatementResult result = new GetStatementResult();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(requestValue.getUsername(), requestValue.getPassword());
        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createGetStatementResult(result);
        }
        if (requestValue.getServiceId() != 1 && requestValue.getServiceId() != 2 && requestValue.getServiceId() != 3) {
            result.setErrorMsg("Услуга не найдена");
            result.setStatus(305);
            return factory.createGetStatementResult(result);
        } else {
            Timestamp from = new Timestamp(requestValue.getDateFrom().toGregorianCalendar().getTimeInMillis());
            Timestamp to = new Timestamp(requestValue.getDateTo().toGregorianCalendar().getTimeInMillis());

            //TRANSACTON LISTNI DB DAN OLAMIZ
            List<PaynetTransaction> transactions = paynetTransactionRepository.findAllByCreatedAtBetween(from, to);

            result.setErrorMsg("Transactions");
            result.setStatus(0);
            for (PaynetTransaction paynetTransaction : transactions) {
                TransactionStatement transactionStatement = new TransactionStatement();
                transactionStatement.setAmount(paynetTransaction.getAmount());
                transactionStatement.setProviderTrnId(paynetTransaction.getTransactionId());
                transactionStatement.setTransactionId(paynetTransaction.getTransactionId());
                GregorianCalendar c1 = new GregorianCalendar();
                c1.setTimeInMillis(paynetTransaction.getTransactionTime().getTime());
                transactionStatement.setTransactionTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(c1));
                result.getStatements().add(transactionStatement);
            }
        }
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(new Timestamp(System.currentTimeMillis()).getTime());
        result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        return factory.createGetStatementResult(result);
    }


    //USERNI BORLIGINI TEKSHIRYAPTI
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetInformationArguments")
    @ResponsePayload
    public JAXBElement<GetInformationResult> getInformation(@RequestPayload JAXBElement<GetInformationArguments> request) throws DatatypeConfigurationException {
        ObjectFactory factory = new ObjectFactory();
        GetInformationArguments requestValue = request.getValue();
        GetInformationResult result = new GetInformationResult();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(requestValue.getUsername(), requestValue.getPassword());

        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createGetInformationResult(result);
        }

        //BU PAYNET BILAN KELISHILIB TO'LOV HAR XIL NARSA UCHUN TO'LANSA KERAK BO'LADI. MISOL UCHUN SIZDA ONLINE UCHUN ALOHIDA,
        // OFFLINE UCHUN ALOHIDA, BOOTCAMP UCHUN ALOHIDA BO'LSA, PAYNET BILAN KELISHIB MIJOZ TANALAGAN TUR UCHUN TO'LOV QILISH UCHUN KERAK
        // (BIZNI HOLATDA TO'LOV FAQAT ID FAQAT 1,2 YOKI 3 SERVICE ID BO'LISHI MUMKIN)
        if (requestValue.getServiceId() != 1 && requestValue.getServiceId() != 2 && requestValue.getServiceId() != 3) {
            result.setErrorMsg("Услуга не найдена");
            result.setStatus(305);
            return factory.createGetInformationResult(result);
        } else {
            String phoneNumber = "";
            for (GenericParam parameter : requestValue.getParameters()) {
                if (parameter.getParamKey().equals("phone_number")) {
                    phoneNumber = parameter.getParamValue().trim();
                    if (!phoneNumber.startsWith("+")) {
                        phoneNumber = "+" + phoneNumber;
                    }
                }
            }
            try {
                if (!phoneNumber.isEmpty() && clientRepository.existsByUsername(phoneNumber)) {
                    result.setErrorMsg("Success");
                    result.setStatus(0);
                } else {
                    result.setStatus(302);
                    result.setErrorMsg("Transaction for the subscriber is not allowed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                result.setStatus(302);
                result.setErrorMsg("Transaction for the subscriber is not allowed");
            }
        }
        GregorianCalendar c1 = new GregorianCalendar();
        c1.setTime(new Date());
        result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c1));
        return factory.createGetInformationResult(result);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ChangePasswordArguments")
    @ResponsePayload
    public JAXBElement<ChangePasswordResult> changePassword(@RequestPayload JAXBElement<ChangePasswordArguments> request) throws DatatypeConfigurationException {
        ObjectFactory factory = new ObjectFactory();
        ChangePasswordArguments requestValue = request.getValue();
        ChangePasswordResult result = new ChangePasswordResult();

        String username = requestValue.getUsername();
        String password = requestValue.getPassword();
        String newPassword = requestValue.getNewPassword();

        //PAYNETNI AUTH QILISH
        GenericResult auth = setAuth(username, password);

        if (auth.getStatus() != 0) {
            result.setErrorMsg(auth.getErrorMsg());
            result.setStatus(auth.getStatus());
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createChangePasswordResult(result);
        }

        //PAYNET USERNI PAROLINI ALMASHTIRAMIZ
        if (!changePaynetPassword(username, newPassword)) {
            result.setErrorMsg("Неверный логин");
            result.setStatus(412);
            result.setTimeStamp(auth.getTimeStamp());
            return factory.createChangePasswordResult(result);
        } else {
            result.setErrorMsg("OK");
            result.setStatus(0);
            GregorianCalendar c1 = new GregorianCalendar();
            c1.setTime(new Date());
            result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c1));
            return factory.createChangePasswordResult(result);
        }
    }

    //PAYNETNI AUTH QILISH
    private GenericResult setAuth(String username, String password) throws DatatypeConfigurationException {
        GenericResult result = new GenericResult();
        try {
            if (!username.equals("Paynet")) throw new Exception("Неверный логин");
            if (authenticatePaynetUser(username, password)) {
                result.setStatus(0);
            } else {
                throw new Exception("Not found");
            }
        } catch (Exception e) {
            result.setErrorMsg("Неверный логин");
            result.setStatus(412);
            GregorianCalendar c1 = new GregorianCalendar();
            c1.setTime(new Date());
            result.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c1));
        }
        return result;
    }


    //PAYNET USERNI TEKSHIRAMIZ. HAMMASI YAXSHI BO'LSA USERNI TIZIMGA KRITIAMIZ VA TRUE QAYTARAMIZ
    private boolean authenticatePaynetUser(String username, String password) {
        Client client = clientRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        boolean passwordMatch = passwordEncoder.matches(password, client.getPassword());
        if (passwordMatch) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(client, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        }
        return false;
    }

    //PAYNET USERNI PAROLINI ALMASHTIRAMIZ
    private boolean changePaynetPassword(String username, String password) {
        if (password == null)
            return false;
        Client client = (Client) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        client.setPassword(passwordEncoder.encode(password));
        clientRepository.save(client);
        return true;
    }

}
