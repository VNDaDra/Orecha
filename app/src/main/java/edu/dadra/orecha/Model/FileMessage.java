package edu.dadra.orecha.Model;

public class FileMessage {
    private String name;
    private String extension;
    private Long sizeInKB;

    public FileMessage() {}

    public FileMessage(String name, String extension, Long sizeInKB) {
        this.name = name;
        this.extension = extension;
        this.sizeInKB = sizeInKB;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getSizeInKB() {
        return sizeInKB;
    }

    public void setSizeInKB(Long sizeInKB) {
        this.sizeInKB = sizeInKB;
    }
}
