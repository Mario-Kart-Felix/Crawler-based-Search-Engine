public class PageScore {

    double words;
    double unstemmed_score;
    double title_score;
    public PageScore()
    {

        title_score = 0;
        words = 0;
        unstemmed_score = 0;

    }
    public double getScore(){
        return title_score+words+unstemmed_score;
    }

}