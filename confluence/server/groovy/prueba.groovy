public enum ColorEnum{
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    MAGENTA("\u001B[35m"),
    DEFAULT("\u001B[0m")

    String value

    ColorEnum(String value){
        this.value = value
    }
}


println(ColorEnum.DEFAULT.value)