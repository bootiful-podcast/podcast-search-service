package search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
class Podcast {

    private Integer id;
    private String uid;
    private String title;
    private Date date;
    private URI episodePhotoUri;
    private String description;
    private URI episodeUri;

}
