package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;


public class Main {
    static final String URL_API = "https://www.infomoney.com.br/wp-json/infomoney/v1/cards";
    static final String URL_MERCADO = "https://www.infomoney.com.br/mercados/";
    static final int LIMITE_NOTICIAS = 20;

    public static void main(String[] args) {

        try {
            List<String> urls = new ArrayList<>();
            List<String> urlsValidas = new ArrayList<>();

            // notícias carregadas no html
            urls.addAll(pegarNoticiasDaPagina());

            // notícias carregadas via api
            int lastPostId = 0;

            while (urlsValidas.size() < LIMITE_NOTICIAS) {
                while (urls.size() <= urlsValidas.size()) {
                    lastPostId = carregarMais(lastPostId, urls);
                }

                String url = urls.get(urlsValidas.size());

                if (processarNoticia(url)) {
                    urlsValidas.add(url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static List<String> pegarNoticiasDaPagina() throws Exception {

        Document doc = Jsoup.connect(URL_MERCADO)
                .userAgent("Mozilla/5.0")
                .get();

        List<String> urls = new ArrayList<>();

        // noticia destaque
        Element destaque = doc.selectFirst(".wp-block-infomoney-blocks-infomoney-special-main a[href]");
        if (destaque != null) {
            urls.add(destaque.attr("href"));
        }

        // noticias cards
        for (Element elemento : doc.select("[data-ds-component=card-sm] a[href], [data-ds-component=card-md] a[href], [data-ds-component=card-lg] a[href]")) {

            String link = elemento.attr("href");

            if (link.equals(URL_MERCADO) || link.equals(URL_MERCADO + "/")) {
                continue;
            }

            if (link.startsWith(URL_MERCADO) && !urls.contains(link)) {
                urls.add(link);
            }
        }

        return urls;
    }

    static int carregarMais(int postId, List<String> urls) throws Exception {

        if (urls.size() >= LIMITE_NOTICIAS) return postId;

        JSONObject payload = new JSONObject();
        payload.put("post_id", postId);
        payload.put("categories", new JSONArray().put(1));
        payload.put("tags", new JSONArray());

        Connection.Response response = Jsoup.connect(URL_API)
                .ignoreContentType(true)
                .header("Content-Type", "application/json")
                .method(Connection.Method.POST)
                .requestBody(payload.toString())
                .execute();

        JSONArray array = new JSONArray(response.body());

        int ultimoId = postId;

        for (int i = 0; i < array.length(); i++) {

            if (urls.size() >= LIMITE_NOTICIAS) break;

            JSONObject obj = array.getJSONObject(i);

            String link = obj.getString("post_permalink");
            int id = obj.getInt("post_id");

            if (link.equals(URL_MERCADO) || link.equals(URL_MERCADO + "/")) {
                continue;
            }

            if (!urls.contains(link)) {
                urls.add(link);
            }

            ultimoId = id;
        }

        return ultimoId;
    }

    static boolean processarNoticia(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            String titulo = verificarTextoNulo(doc.selectFirst("h1"));
            if (titulo.isEmpty()) return false;

            String subtitulo = verificarTextoNulo(doc.selectFirst("h2"));
            if (subtitulo.isEmpty()) return false;

            Element autorElemento = doc.selectFirst("a[href*='/autor/']");
            String autor = autorElemento != null ? autorElemento.text().trim() : "N/A";
            if (autor.isEmpty()) return false;

            Element dataElemento = doc.selectFirst("time");
            String data = dataElemento != null ? dataElemento.text() : "";
            if (data.isEmpty()) return false;

            Element conteudoElemento = doc.selectFirst("article, .article-content, .single-post-content");
            String conteudo = conteudoElemento != null ? conteudoElemento.text().replaceAll("\\s+", " ").trim() : "";
            if (conteudo.isEmpty()) return false;

            System.out.println("\n------------------------");
            System.out.println("URL: " + url);
            System.out.println("Título: " + titulo);
            System.out.println("Subtítulo: " + subtitulo);
            System.out.println("Autor: " + autor);
            System.out.println("Data: " + data);
            System.out.println("Conteúdo: " + conteudo);

            return true;
        } catch (Exception e) {
            System.out.println("Erro ao processar: " + url);
            return false;
        }
    }

    private static String verificarTextoNulo(Element el) {
        if (el == null) return "";
        String text = el.text();
        return text != null ? text.trim() : "";
    }
}
