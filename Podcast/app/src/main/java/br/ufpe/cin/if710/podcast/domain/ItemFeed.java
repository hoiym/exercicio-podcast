package br.ufpe.cin.if710.podcast.domain;

import java.io.Serializable;

// https://stackoverflow.com/questions/2736389/how-to-pass-an-object-from-one-activity-to-another-on-android
// Segundo link acima, se a custom class implementar a interface Serializable, poderá ser passada o objeto
// por meio de putExtra
public class ItemFeed implements Serializable{
    private final String title;
    private final String link;
    private final String pubDate;
    private final String description;
    private final String downloadLink;


    public ItemFeed(String title, String link, String pubDate, String description, String downloadLink) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
        this.downloadLink = downloadLink;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public String getDescription() {
        return description;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    @Override
    public String toString() {
        return title;
    }
}