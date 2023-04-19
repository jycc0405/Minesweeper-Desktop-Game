package minesweeper;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.swing.border.TitledBorder;
import minesweeper.Score.Time;



// This is the main controller class
public class Game implements MouseListener, ActionListener, WindowListener
{
    public static String dbPath;
    // "playing" indicates whether a game is running (true) or not (false).
    private boolean playing;

    private Board board;

    private UI gui;
    
    private Score score;

    private Option Option=new Option();
        
    //------------------------------------------------------------------//        

    public Game()
    {
        // set db path
        String p = "";

        try 
        {
            p = new File(Game.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\\db.accdb";
        }
        catch (URISyntaxException ex) 
        {
            System.out.println("Error loading database file.");
        }

        dbPath =   "jdbc:ucanaccess://" + p;

        
        score = new Score();
        score.populate();
        
        UI.setLook("Nimbus");

        Option.loadOption();

        createBoard(Option.getDiffOption());
        
        this.gui = new UI(board.getRows(), board.getCols(), board.getNumberOfMines(),Option.getDiffOption());
        this.gui.setButtonListeners(this);
                        
        this.playing = false;
        
        gui.setVisible(true);
        
        gui.setIcons();        
        gui.hideAll();

        resumeGame();
    }

    //-----------------Load Save Game (if any)--------------------------//
    
    public void resumeGame()
    {
        if(board.checkSave())
        {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question.png"));      

            int option = JOptionPane.showOptionDialog(null, "저장한 게임을 불러오시겠습니까?",
                            "Saved Game Found", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, question,null,null);

            switch(option) 
            {
                case JOptionPane.YES_OPTION:      
      
                    //load board's state
                    Pair p = board.loadSaveGame();
                    
                    //set button's images
                    setButtonImages();
                    
                    //load timer's value                                        
                    gui.setTimePassed((int)p.getKey());

                    //load mines value
                    gui.setMines((int)p.getValue());
                    
                    gui.startTimer();
                    
                    playing = true;
                    break;

                case JOptionPane.NO_OPTION:
                    board.deleteSavedGame();
                    break;
                    
                case JOptionPane.CLOSED_OPTION:
                    board.deleteSavedGame();
                    break;
            }
        }
    }


    //-------------------------------------------------//
    public void setButtonImages()
    {
        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();
        
        for( int y=0 ; y<board.getRows() ; y++ ) 
        {
            for( int x=0 ; x<board.getCols() ; x++ ) 
            {
                buttons[x][y].setIcon(null);
                
                if (cells[x][y].getContent().equals(""))
                {
                    buttons[x][y].setIcon(gui.getIconTile());
                }
                else if (cells[x][y].getContent().equals("F"))
                {
                    buttons[x][y].setIcon(gui.getIconFlag());
                    buttons[x][y].setBackground(Color.blue);
                }
                else if (cells[x][y].getContent().equals("0"))
                {
                    buttons[x][y].setBackground(Color.lightGray);
                }
                else
                {
                    buttons[x][y].setBackground(Color.lightGray);                    
                    buttons[x][y].setText(cells[x][y].getContent());
                    gui.setTextColor(buttons[x][y]);                                        
                }
            }
        }
    }
    
    
    //------------------------------------------------------------//
        
    public void createBoard(int diffOption)
    {
        int[][] sizeOfDiff={{9,9,10},{16,16,40},{30,16,99}};

        int[] size=sizeOfDiff[diffOption];
        System.out.println(size[0]);
        System.out.println(size[1]);
        System.out.println(size[2]);
        // Create a new board        
        int mines = size[2];

        int r = size[0];
        int c = size[1];
                
        this.board = new Board(mines, r, c);        
    }
    

    //---------------------------------------------------------------//
    public void newGame()
    {                
        this.playing = false;

        Option.loadOption();
                                
        createBoard(Option.getDiffOption());
        
        gui.interruptTimer();
        gui.resetTimer();        
        gui.initGame();
        gui.setMines(board.getNumberOfMines());
    }
    //------------------------------------------------------------------------------//
    
    public void restartGame()
    {
        this.playing = false;
        
        board.resetBoard();
        
        gui.interruptTimer();
        gui.resetTimer();        
        gui.initGame();
        gui.setMines(board.getNumberOfMines());
    }

    //------------------------------------------------------------------------------//

        
    //------------------------------------------------------------------------------//    
    private void endGame()
    {
        playing = false;
        showAll();

        score.save();
    }

    
    //-------------------------GAME WON AND GAME LOST ---------------------------------//
    
    public void gameWon()
    {
        score.incCurrentStreak();
        score.incCurrentWinningStreak();
        score.incGamesWon();
        score.incGamesPlayed();
        
        gui.interruptTimer();
        endGame();
        //----------------------------------------------------------------//
        
        
        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);
        
        //------MESSAGE-----------//
        JLabel message = new JLabel("축하합니다! 게임 클리어!", SwingConstants.CENTER);
                
        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();
        statistics.setLayout(new GridLayout(6,1,0,10));
        
        ArrayList<Time> bTimes = score.getBestTimes();
        
        if (bTimes.isEmpty() || (bTimes.get(0).getTimeValue() > gui.getTimePassed()))
        {
            statistics.add(new JLabel("    현재 레벨에서 신기록 달성!    "));
        }
        
        score.addTime(gui.getTimePassed(), new Date(System.currentTimeMillis()));
                
        JLabel time = new JLabel("  시간:  " + Integer.toString(gui.getTimePassed()) + " 초            날짜:  " + new Date(System.currentTimeMillis()));
        
        JLabel bestTime = new JLabel();
        
        
        if (bTimes.isEmpty())
        {
            bestTime.setText("  신기록:  ---                  날짜:  ---");
        }
        else
        {
            bestTime.setText("  신기록:  " + bTimes.get(0).getTimeValue() + " 초            날짜:  " + bTimes.get(0).getDateValue());
        }
        
        JLabel gPlayed = new JLabel("  총 게임 횟수:  " + score.getGamesPlayed());
        JLabel gWon = new JLabel("  이긴 게임 횟수:  " + score.getGamesWon());
        JLabel gPercentage = new JLabel("  승률:  " + score.getWinPercentage() + "%");
        
        statistics.add(time);
        statistics.add(bestTime);
        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);
        
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);        
        statistics.setBorder(loweredetched);
        
        
        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,2,10,0));
        
        JButton exit = new JButton("나가기");
        JButton playAgain = new JButton("재시작");

        
        exit.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            windowClosing(null);
        });        
        playAgain.addActionListener((ActionEvent e) -> {
            dialog.dispose();            
            newGame();
        });        
        
        
        buttons.add(exit);
        buttons.add(playAgain);
        
        //--------DIALOG-------------//
        
        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(message, BorderLayout.NORTH);
        c.add(statistics, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);
        
        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    dialog.dispose();
                    newGame();
            }
            }
        );

        dialog.setTitle("승리");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);                        
    }
    
    public void gameLost()
    {
        score.decCurrentStreak();
        score.incCurrentLosingStreak();
        score.incGamesPlayed();
        
        gui.interruptTimer();
        
        endGame();
        
        //----------------------------------------------------------------//

        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);
        
        //------MESSAGE-----------//
        JLabel message = new JLabel("당신은 죽었습니다!", SwingConstants.CENTER);
                
        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();
        statistics.setLayout(new GridLayout(5,1,0,10));
        
        JLabel time = new JLabel("  시간:  " + Integer.toString(gui.getTimePassed()) + " 초");
        
        JLabel bestTime = new JLabel();
        
        ArrayList<Time> bTimes = score.getBestTimes();
        
        if (bTimes.isEmpty())
        {
            bestTime.setText("                        ");
        }
        else
        {
            bestTime.setText("  신기록:  " + bTimes.get(0).getTimeValue() + " 초            날짜:  " + bTimes.get(0).getDateValue());
        }
        
        JLabel gPlayed = new JLabel("  총 게임 횟수:  " + score.getGamesPlayed());
        JLabel gWon = new JLabel("  이긴 게임 횟수:  " + score.getGamesWon());
        JLabel gPercentage = new JLabel("  승률:  " + score.getWinPercentage() + "%");
        
        statistics.add(time);
        statistics.add(bestTime);
        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);
        
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);        
        statistics.setBorder(loweredetched);
        
        
        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,3,2,0));
        
        JButton exit = new JButton("나가기");
        JButton restart = new JButton("현재 게임 재시작");
        JButton playAgain = new JButton("새로운 게임 시작");

        
        exit.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            windowClosing(null);
        });        
        restart.addActionListener((ActionEvent e) -> {
            dialog.dispose();            
            restartGame();
        });        
        playAgain.addActionListener((ActionEvent e) -> {
            dialog.dispose();            
            newGame();
        });        
        
        
        buttons.add(exit);
        buttons.add(restart);
        buttons.add(playAgain);
        
        //--------DIALOG-------------//
        
        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(message, BorderLayout.NORTH);
        c.add(statistics, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);
        
        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    dialog.dispose();
                    newGame();
            }
            }
        );
        
        dialog.setTitle("패배");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);        
    }
    
    
    //--------------------------------SCORE BOARD--------------------------------------//
    public void showScore()
    {
        //----------------------------------------------------------------//
                
        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);

        //-----BEST TIMES--------//
        
        JPanel bestTimes = new JPanel();
        bestTimes.setLayout(new GridLayout(5,1));
        
        ArrayList<Time> bTimes = score.getBestTimes();
        
        for (int i = 0; i < bTimes.size(); i++)
        {
            JLabel t = new JLabel("  " + bTimes.get(i).getTimeValue() + "           " + bTimes.get(i).getDateValue());            
            bestTimes.add(t);
        }
        
        if (bTimes.isEmpty())
        {
            JLabel t = new JLabel("                               ");            
            bestTimes.add(t);
        }
        
        TitledBorder b = BorderFactory.createTitledBorder("Best Times");
        b.setTitleJustification(TitledBorder.LEFT);

        bestTimes.setBorder(b);
                
        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();
        
        statistics.setLayout(new GridLayout(6,1,0,10));        
        
        JLabel gPlayed = new JLabel("  총 게임 횟수:  " + score.getGamesPlayed());
        JLabel gWon = new JLabel("  이긴 횟수:  " + score.getGamesWon());
        JLabel gPercentage = new JLabel("  승률:  " + score.getWinPercentage() + "%");
        JLabel lWin = new JLabel("  최장 연승 횟수:  " + score.getLongestWinningStreak());
        JLabel lLose = new JLabel("  최장 연패 횟수:  " + score.getLongestLosingStreak());
        JLabel currentStreak = new JLabel("  현재 연승 횟수:  " + score.getCurrentStreak());

        
        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);
        statistics.add(lWin);
        statistics.add(lLose);
        statistics.add(currentStreak);
                        
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);        
        statistics.setBorder(loweredetched);
        
        
        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,2,10,0));
        
        JButton close = new JButton("종료");
        JButton reset = new JButton("초기화");

        
        close.addActionListener((ActionEvent e) -> {
            dialog.dispose();
        });        
        reset.addActionListener((ActionEvent e) -> {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question.png"));      

            int option = JOptionPane.showOptionDialog(null, "정보창을 초기화하시겠습니까?",
                            "정보창 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, question,null,null);

            switch(option) 
            {
                case JOptionPane.YES_OPTION:      

                    score.resetScore();
                    score.save();
                    dialog.dispose();
                    showScore();
                    break;

                case JOptionPane.NO_OPTION: 
                    break;
            }
        });        
        
        buttons.add(close);
        buttons.add(reset);
        
        if (score.getGamesPlayed() == 0)
            reset.setEnabled(false);
        
        //--------DIALOG-------------//
        
        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(bestTimes, BorderLayout.WEST);
        c.add(statistics, BorderLayout.CENTER);        
        c.add(buttons, BorderLayout.SOUTH);
        
        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        dialog.setTitle("Minesweeper Statistics - Haris Muneer");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);                        
    }

    //------------------------------------------------------------------------------//



    public void showOption()
    {
        //----------------------------------------------------------------//

        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);


        //-----option-----------//


        JPanel option = new JPanel();

        option.setLayout(new GridLayout(2,1,10,10));

        JPanel diffOption = new JPanel();

        TitledBorder b = BorderFactory.createTitledBorder("난이도 설정");
        b.setTitleJustification(TitledBorder.LEFT);

        diffOption.setBorder(b);


        diffOption.setLayout(new GridLayout(1,3,10,10));

        JRadioButton diff1= new JRadioButton("쉬움");
        JRadioButton diff2= new JRadioButton("보통");
        JRadioButton diff3= new JRadioButton("어려움");

        diff1.setSelected(true);

        ButtonGroup diffGroupRd = new ButtonGroup();

        diffGroupRd.add(diff1);
        diffGroupRd.add(diff2);
        diffGroupRd.add(diff3);


        diffOption.add(diff1);
        diffOption.add(diff2);
        diffOption.add(diff3);

        JPanel qOption = new JPanel();

        TitledBorder b1 = BorderFactory.createTitledBorder("물음표 표식 사용");
        b1.setTitleJustification(TitledBorder.LEFT);

        qOption.setBorder(b1);


        qOption.setLayout(new GridLayout(1,1,10,10));

        JCheckBox chk = new JCheckBox("선택 시 물음표 표식 사용 가능",false);

        qOption.add(chk);

        option.add(diffOption);
        option.add(qOption);

        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,2,10,0));

        JButton apply = new JButton("적용");
        JButton cancel = new JButton("취소");


        apply.addActionListener((ActionEvent e) -> {
            dialog.dispose();
        });
        cancel.addActionListener((ActionEvent e) -> {
            dialog.dispose();
        });


        buttons.add(apply);
        buttons.add(cancel);

        //--------DIALOG-------------//

        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(option, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);

        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        dialog.setTitle("옵션창");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);
    }
    
    //------------------------------------------------------------------------------//


    // Shows the "solution" of the game.
    private void showAll()
    {
        String cellSolution;
        
        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();

        for (int x=0; x<board.getRows(); x++ )
        {
            for (int y=0; y<board.getCols(); y++ )
            {
                cellSolution = cells[x][y].getContent();

                // Is the cell still unrevealed
                if( cellSolution.equals("") ) 
                {
                    buttons[x][y].setIcon(null);
                    
                    // Get Neighbours
                    cellSolution = Integer.toString(cells[x][y].getSurroundingMines());

                    // Is it a mine?
                    if(cells[x][y].getMine()) 
                    {
                        cellSolution = "M";
                        
                        //mine
                        buttons[x][y].setIcon(gui.getIconMine());
                        buttons[x][y].setBackground(Color.lightGray);                        
                    }
                    else
                    {
                        if(cellSolution.equals("0"))
                        {
                            buttons[x][y].setText("");                           
                            buttons[x][y].setBackground(Color.lightGray);
                        }
                        else
                        {
                            buttons[x][y].setBackground(Color.lightGray);
                            buttons[x][y].setText(cellSolution);
                            gui.setTextColor(buttons[x][y]);
                        }
                    }
                }

                // This cell is already flagged!
                else if( cellSolution.equals("F") ) 
                {
                    // Is it correctly flagged?
                    if(!cells[x][y].getMine()) 
                    {
                        buttons[x][y].setBackground(Color.orange);
                    }
                    else
                        buttons[x][y].setBackground(Color.green);
                }
                
            }
        }
    }
    

    //-------------------------------------------------------------------------//
    
    //-------------------------------------------------------------------------//    
    

    //-------------------------------------------------------------------------//

    
    //--------------------------------------------------------------------------//
    
    public boolean isFinished()
    {
        boolean isFinished = true;
        String cellSolution;

        Cell cells[][] = board.getCells();
        
        for( int x = 0 ; x < board.getRows() ; x++ )
        {
            for( int y = 0 ; y < board.getCols() ; y++ )
            {
                // If a game is solved, the content of each Cell should match the value of its surrounding mines
                cellSolution = Integer.toString(cells[x][y].getSurroundingMines());
                
                if(cells[x][y].getMine()) 
                    cellSolution = "F";

                // Compare the player's "answer" to the solution.
                if(!cells[x][y].getContent().equals(cellSolution))
                {
                    //This cell is not solved yet
                    isFinished = false;
                    break;
                }
            }
        }

        return isFinished;
    }

 
    //Check the game to see if its finished or not
    private void checkGame()
    {
        if(isFinished()) 
        {            
            gameWon();
        }
    }
   
    //----------------------------------------------------------------------/

    /*
     * If a player clicks on a confirmed number cell, it checks the cells around it.
     */
    public String checkNeighbours(int xCo, int yCo) {

        int neighbours;
        boolean isMine;
        JButton buttons[][] = gui.getButtons();
        String result = "";

        for (int x = board.makeValidCoordinateX(xCo - 1); x <= board.makeValidCoordinateX(xCo + 1); x++) {
            for (int y = board.makeValidCoordinateY(yCo - 1); y <= board.makeValidCoordinateY(yCo + 1); y++) {
                if (x != xCo || y != yCo) {
                    if (!board.getCells()[x][y].getContent().equals("F")) {
                        buttons[x][y].setIcon(null);
                        isMine = board.getCells()[x][y].getMine();
                        neighbours = board.getCells()[x][y].getSurroundingMines();

                        if (isMine) {
                            //red mine
                            buttons[x][y].setIcon(gui.getIconRedMine());
                            buttons[x][y].setBackground(Color.red);
                            board.getCells()[x][y].setContent("M");

                            result = "lose";
                        } else if (board.getCells()[x][y].getContent().equals("")) {
                            // The player has clicked on a number.
                            board.getCells()[x][y].setContent(Integer.toString(neighbours));
                            buttons[x][y].setText(Integer.toString(neighbours));
                            gui.setTextColor(buttons[x][y]);

                            if (neighbours == 0) {
                                // Show all surrounding cells.
                                buttons[x][y].setBackground(Color.lightGray);
                                buttons[x][y].setText("");
                                findZeroes(x, y);
                            } else {
                                buttons[x][y].setBackground(Color.lightGray);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }



    /*
     * If a player clicks on a zero, all surrounding cells ("neighbours") must revealed.
     * This method is recursive: if a neighbour is also a zero, his neighbours must also be revealed.
     */
    public void findZeroes(int xCo, int yCo)
    {
        int neighbours;
        
        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();

        // Columns
        for(int x = board.makeValidCoordinateX(xCo - 1) ; x <= board.makeValidCoordinateX(xCo + 1) ; x++) 
        {
            // Rows
            for(int y = board.makeValidCoordinateY(yCo - 1) ; y <= board.makeValidCoordinateY(yCo + 1) ; y++) 
            {
                // Only unrevealed cells need to be revealed.
                if(cells[x][y].getContent().equals("")) 
                {
                    // Get the neighbours of the current (neighbouring) cell.
                    neighbours = cells[x][y].getSurroundingMines();

                    // Reveal the neighbours of the current (neighbouring) cell
                    cells[x][y].setContent(Integer.toString(neighbours));

                    if (!cells[x][y].getMine())
                        buttons[x][y].setIcon(null);                        
                    
                    // Is this (neighbouring) cell a "zero" cell itself?
                    if(neighbours == 0)
                    {                        
                        // Yes, give it a special color and recurse!
                        buttons[x][y].setBackground(Color.lightGray);
                        buttons[x][y].setText("");
                        findZeroes(x, y);
                    }
                    else
                    {
                        // No, give it a boring gray color.
                        buttons[x][y].setBackground(Color.lightGray);
                        buttons[x][y].setText(Integer.toString(neighbours));
                        gui.setTextColor(buttons[x][y]);                        
                    }
                }
            }
        }
    }
    //-----------------------------------------------------------------------------//
    //This function is called when clicked on closed button or exit
    @Override
    public void windowClosing(WindowEvent e) 
    {
        if (playing)
        {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question.png"));      

            Object[] options = {"저장","삭제","취소"};

            int quit = JOptionPane.showOptionDialog(null, "현재 게임을 어떻게 하시겠습니까?",
                            "New Game", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, question, options, options[2]);

            switch(quit) 
            {
                //save
                case JOptionPane.YES_OPTION:
                    
                    gui.interruptTimer();
                    score.save();
                    
                    JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    panel.add(new JLabel("저장중... 잠시만 기다려주십시오...", SwingConstants.CENTER));
                    dialog.add(panel);
                    dialog.setTitle("저장중...");
                    dialog.pack();
                    dialog.setLocationRelativeTo(gui);                    
                    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                       @Override
                       protected Void doInBackground() throws Exception 
                       {
                            board.saveGame(gui.getTimePassed(), gui.getMines());                
                            return null;
                       }
                       
                       @Override
                       protected void done(){
                           dialog.dispose();                           
                       }                       
                    };
                            
                    worker.execute();
                    dialog.setVisible(true);
                                                            
                    System.exit(0);
                    break;
                
                //dont save                    
                case JOptionPane.NO_OPTION:
                    score.incGamesPlayed();
                    score.save();
                    System.exit(0);
                    break;
                    
                case JOptionPane.CANCEL_OPTION: break;
            }
        }
        else
            System.exit(0);
    }
    
    //-----------------------------------------------------------------------//

    @Override
    public void actionPerformed(ActionEvent e) {        
        JMenuItem menuItem = (JMenuItem) e.getSource();

        if (menuItem.getName().equals("New Game"))
        {
            if (playing)
            {
                ImageIcon question = new ImageIcon(getClass().getResource("/resources/question.png"));      

                Object[] options = {"종료 후 새로운 게임","현재 게임 재시작","계속하기"};
                
                int startNew = JOptionPane.showOptionDialog(null, "현재 게임을 어떻게 하시겠습니까?",
                                "New Game", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, question, options, options[2]);

                switch(startNew) 
                {
                    case JOptionPane.YES_OPTION:      
                        
                        // Initialize the new game.
                        newGame();
                        score.incGamesPlayed();
                        score.save();
                        break;

                    case JOptionPane.NO_OPTION: 
                        score.incGamesPlayed();   
                        score.save();
                        restartGame();
                        break;
                    
                    case JOptionPane.CANCEL_OPTION: break;
                }
            }
        }
        
        else if (menuItem.getName().equals("Exit"))
        {
            windowClosing(null);
        }
        
        //Statistics
        else if (menuItem.getName().equals("Statistics"))
        {
            showScore();
        }

        else if (menuItem.getName().equals("Option"))
        {
            System.out.println("heol");
            showOption();
        }
    }
    
    
    //--------------------------------------------------------------------------//
        
    //Mouse Click Listener
    @Override
    public void mouseClicked(MouseEvent e)
    {
        // start timer on first click
        if(!playing)
        {
            gui.startTimer();
            playing = true;
        }
        
        if (playing)
        {
            //Get the button's name
            JButton button = (JButton)e.getSource();

            // Get coordinates (button.getName().equals("x,y")).
            String[] co = button.getName().split(",");

            int x = Integer.parseInt(co[0]);
            int y = Integer.parseInt(co[1]);

            // Get cell information.
            boolean isMine = board.getCells()[x][y].getMine();
            int neighbours = board.getCells()[x][y].getSurroundingMines();

            // Left Click
            if (SwingUtilities.isLeftMouseButton(e)) 
            {
                if (!board.getCells()[x][y].getContent().equals("F")&&board.getCells()[x][y].getContent().equals(""))
                {
                    button.setIcon(null);

                    //Mine is clicked.
                    if(isMine) 
                    {  
                        //red mine
                        button.setIcon(gui.getIconRedMine());
                        button.setBackground(Color.red);
                        board.getCells()[x][y].setContent("M");

                        gameLost();
                    }
                    else 
                    {
                        // The player has clicked on a number.
                        board.getCells()[x][y].setContent(Integer.toString(neighbours));
                        button.setText(Integer.toString(neighbours));
                        gui.setTextColor(button);

                        if( neighbours == 0 ) 
                        {
                            // Show all surrounding cells.
                            button.setBackground(Color.lightGray);
                            button.setText("");
                            findZeroes(x, y);
                        } 
                        else 
                        {
                            button.setBackground(Color.lightGray);
                        }
                    }
                }else if(!board.getCells()[x][y].getContent().equals("")&&!board.getCells()[x][y].getContent().equals("F"))
                {
                    if (board.getSurroundingFlagNumber(x,y)>=Integer.parseInt(board.getCells()[x][y].getContent()))
                    {
                        String result=checkNeighbours(x,y);
                        if(result.equals("lose"))
                            gameLost();
                    }
                }

            }
            // Right Click
            else if (SwingUtilities.isRightMouseButton(e)) 
            {
                if(board.getCells()[x][y].getContent().equals("F")) 
                {   
                    board.getCells()[x][y].setContent("");
                    button.setText("");
                    button.setBackground(new Color(0,110,140));

                    //simple blue

                    button.setIcon(gui.getIconTile());
                    gui.incMines();
                }
                else if (board.getCells()[x][y].getContent().equals("")) 
                {
                    board.getCells()[x][y].setContent("F");
                    button.setBackground(Color.blue);

                    button.setIcon(gui.getIconFlag());
                    gui.decMines();
                }
            }

            checkGame();
        }
    }

    //-------------------------RELATED TO SCORES----------------------//


    
    //---------------------EMPTY FUNCTIONS-------------------------------//
    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }    

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}
