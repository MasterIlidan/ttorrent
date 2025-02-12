package ru.students.config;

import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.students.entity.Torrent;
import ru.students.repository.TorrentRepository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class WebSecurityConf {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests((authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/forum").permitAll()
                        .requestMatchers("/getPeers").permitAll()
                        .requestMatchers("/getSizeOfTracker").permitAll()
                        .requestMatchers("/getHash").permitAll()
                        .requestMatchers("/announce").permitAll()
                        .anyRequest().authenticated())).formLogin(form ->
                        form.loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/")
                                .permitAll())
                .logout(logout -> logout.logoutUrl("logout").permitAll()).csrf().disable();
        return httpSecurity.build();
    }

    @Bean
    public Tracker createTracker(TorrentRepository torrentRepository) throws IOException {
        Tracker tracker = new Tracker(6969);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".torrent");
            }
        };
        List<Torrent> torrentList = torrentRepository.findAll();
        for (Torrent torrent:torrentList) {
            if (torrent.getStatus().equals(Torrent.Status.NEW)) continue;
            File file = Paths.get("C:\\Users\\MasterIlidan\\IdeaProjects\\ttorrent\\staging", torrent.getFileName()).toFile();
            tracker.announce(TrackedTorrent.load(file));
        }

//Also you can enable accepting foreign torrents.
//if tracker accepts request for unknown torrent it starts tracking the torrent automatically
        tracker.setAcceptForeignTorrents(false);

// Once done, you just have to start the tracker's main operation loop:
        tracker.start(true);
        return tracker;
    }

}
