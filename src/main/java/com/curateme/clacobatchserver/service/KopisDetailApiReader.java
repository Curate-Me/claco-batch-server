package com.curateme.clacobatchserver.service;

import com.curateme.clacobatchserver.entity.Concert;
import com.curateme.clacobatchserver.repository.ConcertRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KopisDetailApiReader implements Tasklet {

    private final ConcertRepository concertRepository;

    public KopisDetailApiReader(ConcertRepository concertRepository) {
        this.concertRepository = concertRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<Concert> beforeEntities = concertRepository.findAll();

        for (Concert concert : beforeEntities) {
            fetchAndSave(concert.getMt20id(), concert);
        }

        return RepeatStatus.FINISHED;
    }

    private void fetchAndSave(String mt20id, Concert concert) {
        try {
            String urlString = String.format(
                "http://www.kopis.or.kr/openApi/restful/pblprfr/%s?service=f222668534db409b8769f640387de9c3",
                mt20id
            );
            System.out.println("url = " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String xmlResponse = response.toString();
                System.out.println("Response: " + xmlResponse);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

                document.getDocumentElement().normalize();
                NodeList nodeList = document.getElementsByTagName("db");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        String prfcast = getTagValue("prfcast", element);
                        String prfcrew = getTagValue("prfcrew", element);
                        String prfruntime = getTagValue("prfruntime", element);
                        String prfage = getTagValue("prfage", element);
                        String entrpsnm = getTagValue("entrpsnm", element);
                        String entrpsnmP = getTagValue("entrpsnmP", element);
                        String entrpsnmA = getTagValue("entrpsnmA", element);
                        String entrpsnmH = getTagValue("entrpsnmH", element);
                        String entrpsnmS = getTagValue("entrpsnmS", element);
                        String pcseguidance = getTagValue("pcseguidance", element);
                        String visit = getTagValue("visit", element);
                        String child = getTagValue("child", element);
                        String daehakro = getTagValue("daehakro", element);
                        String festival = getTagValue("festival", element);
                        String musicallicense = getTagValue("musicallicense", element);
                        String musicalcreate = getTagValue("musicalcreate", element);
                        String updatedate = getTagValue("updatedate", element);
                        String dtguidance = getTagValue("dtguidance", element);

                        NodeList styurlsList = element.getElementsByTagName("styurls");
                        if (styurlsList.getLength() > 0) {
                            Element styurlsElement = (Element) styurlsList.item(0);
                            NodeList styurlList = styurlsElement.getElementsByTagName("styurl");
                            if (styurlList.getLength() > 0) {
                                String styurl = styurlList.item(0).getTextContent();
                                concert.setStyurl(styurl);
                            }
                        }

                        concert.setAdditionalConcertDetails(
                            prfcast, prfcrew, prfruntime, prfage, entrpsnm, entrpsnmP, entrpsnmA, entrpsnmH,
                            entrpsnmS, pcseguidance, visit, child, daehakro, festival, musicallicense,
                            musicalcreate, updatedate, dtguidance
                        );

                        concertRepository.save(concert);
                    }
                }
            } else {
                System.out.println("Error: Received HTTP " + responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node != null ? node.getNodeValue() : "";
    }
}
